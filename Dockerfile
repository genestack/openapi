# syntax=docker/dockerfile:1.29
#
# ODM's OpenAPI specs and the three artifacts built from them: the Swagger UI image
# (genestack/swagger), the Python client (PyPI, or TestPyPI for a development version)
# and the R client (Nexus). The build context is this repo, filtered by .dockerignore.
# processors-controller's spec comes in as the `processors_controller_spec` named
# context, a directory with openapi.yaml: CI passes its odm-oas image, compose its
# checkout's api/odm-oas. The Makefile builds these targets:
#
#   swagger               the image CI publishes and compose runs
#   python-client-upload  the Python client's sdist, uploaded with twine
#   r-client-upload       the R client's R CMD build tarball, uploaded with curl
#   test                  tox run-parallel on the Python client, R CMD check on the R one
#
# OPENAPI_VERSION is the version of all three. Gradle reads it from the environment
# (build.gradle.kts), and the uploads pick the release or development repository by it.

ARG JDK_IMAGE=eclipse-temurin:25.0.5_8-jdk-alpine
ARG PYTHON_IMAGE=python:3.15.2-alpine
ARG R_IMAGE=rocker/r-ver:4.6.2
ARG SWAGGER_UI_IMAGE=swaggerapi/swagger-ui:v5.35.0

FROM ${JDK_IMAGE} AS build

WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
COPY buildSrc/ buildSrc/
COPY openapi/ openapi/
# processors_controller_spec is the named build context the Makefile and compose pass.
# hadolint ignore=DL3022
COPY --from=processors_controller_spec openapi.yaml openapi/v1/processorsController.yaml

ARG OPENAPI_VERSION

# Merges openapi/v1 into odmApi.yaml and generates generated/python and generated/r from
# it. Gradle's cross-process locks do not reach another build's container, so the caches
# are this repo's own (id) and one build uses them at a time (locked).
RUN --mount=type=cache,id=openapi-gradle-caches,target=/root/.gradle/caches,sharing=locked \
    --mount=type=cache,id=openapi-gradle-wrapper,target=/root/.gradle/wrapper,sharing=locked \
    : "${OPENAPI_VERSION:?is not set}" \
 && ./gradlew generateAll --no-daemon

FROM ${PYTHON_IMAGE} AS python-client

WORKDIR /app

COPY requirements.txt ./
RUN --mount=type=cache,target=/root/.cache \
    python3 -m pip install -r requirements.txt

COPY --from=build /app/generated/python generated/python
WORKDIR /app/generated/python

RUN python3 setup.py sdist

FROM python-client AS python-client-upload

ARG OPENAPI_VERSION

# Releases go to PyPI, development builds to TestPyPI.
RUN --mount=type=secret,id=PYPI_TOKEN,env=PYPI_TOKEN \
    --mount=type=secret,id=PYPI_TOKEN_TEST,env=PYPI_TOKEN_TEST \
    if echo "${OPENAPI_VERSION}" | grep -Exq "^([0-9]+(.)?){3}$"; then \
        repository=https://upload.pypi.org/legacy/; token=${PYPI_TOKEN}; \
    else \
        repository=https://test.pypi.org/legacy/; token=${PYPI_TOKEN_TEST}; \
    fi \
 && TWINE_USERNAME=__token__ TWINE_PASSWORD="pypi-${token}" \
    twine upload dist/* --repository-url "${repository}"

FROM ${R_IMAGE} AS r-client

WORKDIR /app

COPY requirements.R ./
# The image has the compilers. The source packages build against these headers.
RUN --mount=type=cache,target=/root/.cache \
    apt-get update \
 && apt-get install -y libssl-dev libcurl4-gnutls-dev curl \
 && Rscript requirements.R

COPY --from=build /app/generated/r generated/r
WORKDIR /app/generated/r

RUN R CMD build .

FROM r-client AS r-client-upload

ARG OPENAPI_VERSION
ARG R_REGISTRY_RELEASES
ARG R_REGISTRY_SNAPSHOTS

# Releases go to the releases repository, development builds to the snapshots one.
RUN --mount=type=secret,id=NEXUS_USER,env=NEXUS_USER \
    --mount=type=secret,id=NEXUS_PASSWORD,env=NEXUS_PASSWORD \
    : "${R_REGISTRY_RELEASES:?is not set}" "${R_REGISTRY_SNAPSHOTS:?is not set}" \
 && if echo "${OPENAPI_VERSION}" | grep -Exq "^([0-9]+(.)?){3}$"; then \
        registry=${R_REGISTRY_RELEASES}; \
    else \
        registry=${R_REGISTRY_SNAPSHOTS}; \
    fi \
 && archive=$(find . -name '*.tar.gz' | sed 's|./||') \
 && curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
    --upload-file "${archive}" "${registry}/src/contrib/${archive}"

FROM python-client AS python-client-test

RUN python3 -m tox run-parallel --result-json /tox-result.json

FROM r-client AS r-client-test

RUN R CMD check ./*.tar.gz --no-manual

# Both checks' reports. Copying them makes one target run both checks, in parallel.
FROM scratch AS test

COPY --from=python-client-test /tox-result.json /python/
COPY --from=r-client-test /app/generated/r/odmApi.Rcheck/00check.log /r/

# Last, so that a plain build with the named context gives this image.
FROM ${SWAGGER_UI_IMAGE} AS swagger

COPY --from=build /app/openapi/v1/ /usr/share/nginx/html/yaml/
COPY openapi/swagger/fs /

# Swagger UI lists each application's spec. The merged odmApi.yaml stays out.
RUN rm -f /usr/share/nginx/html/yaml/odmApi.yaml
# rewrite_entrypoint.sh needs bash. It writes /genestack-docker-entrypoint.sh, which lists
# the specs in Swagger UI's URLS. sqlite-libs is upgraded from Alpine edge, ahead of the
# base image's.
RUN apk add bash --no-cache && \
    rewrite_entrypoint.sh && \
    apk del bash && \
    apk upgrade --no-cache --repository http://dl-cdn.alpinelinux.org/alpine/edge/main sqlite-libs

# The helper page loads the specs through a relative path.
RUN ln -s /usr/share/nginx/html/yaml /usr/share/nginx/html/helper/yaml

ENTRYPOINT ["/genestack-docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
