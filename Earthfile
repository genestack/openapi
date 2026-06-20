VERSION 0.8

ARG --global --required HARBOR_DOCKER_REGISTRY
ARG --global --required RAW_REGISTRY_RELEASES
ARG --global --required RAW_REGISTRY_SNAPSHOTS

build:
    FROM eclipse-temurin:25.0.3_9-jdk-alpine
    DO github.com/genestack/earthly-libs:6e90f15c1b437e0bfdf6f95786cac47fb5c0c7e9+GRADLE_PREPARE

    CACHE /root/.gradle/caches
    CACHE /root/.gradle/wrapper

    COPY --dir openapi gradle gradlew buildSrc build.gradle.kts settings.gradle.kts .

    ARG --required OPENAPI_VERSION
    ARG --required PROCESSORS_CONTROLLER_VERSION
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            ./gradlew \
                generateAll \
                --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated
    SAVE ARTIFACT openapi/v1

python-api-client:
    FROM python:3.14.6-alpine
    DO github.com/genestack/earthly-libs:6e90f15c1b437e0bfdf6f95786cac47fb5c0c7e9+PYTHON_PREPARE

    CACHE /root/.cache

    COPY requirements.txt .
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            pypi-login.sh && \
            python3 \
                -m pip install \
                -r requirements.txt && \
            pypi-clean.sh

    COPY +build/generated generated
    WORKDIR generated/python

    # Test and build python client
    RUN \
        python3 -m tox run-parallel && \
        python3 setup.py sdist

    ARG --required OPENAPI_VERSION
    IF echo ${OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        ARG PYPI_REPOSITORY_INTERNAL="nexus-pypi-releases"
        ARG PYPI_REPOSITORY_PUBLIC="pypi"
    ELSE
        ARG PYPI_REPOSITORY_INTERNAL="nexus-pypi-snapshots"
        ARG PYPI_REPOSITORY_PUBLIC="testpypi"
    END

    RUN --push \
        --secret PYPI_TOKEN \
        --secret PYPI_TOKEN_TEST \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            pypi-login.sh && \
            twine upload dist/* -r ${PYPI_REPOSITORY_INTERNAL} && \
            twine upload dist/* -r ${PYPI_REPOSITORY_PUBLIC} && \
            pypi-clean.sh

r-api-client:
    FROM rocker/r-ver:4.5.3
    WORKDIR /app

    CACHE /root/.cache

    COPY requirements.R .

    # Gcc and other stuff for R source packages building
    RUN \
        apt update && \
        apt install -y libssl-dev libcurl4-gnutls-dev curl && \
        Rscript requirements.R

    COPY +build/generated generated
    WORKDIR generated/r

    # Test and build R client
    RUN \
        R CMD build . && \
        R CMD check *.tar.gz --no-manual

    ARG --required R_REGISTRY_RELEASES
    ARG --required R_REGISTRY_SNAPSHOTS

    ARG --required OPENAPI_VERSION
    IF echo ${OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        ARG R_REGISTRY=${R_REGISTRY_RELEASES}
    ELSE
        ARG R_REGISTRY=${R_REGISTRY_SNAPSHOTS}
    END

    # Push R client
    RUN --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
           export archive=$(find . | grep tar.gz | sed 's|./||') && \
           curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
              --upload-file "${archive}" "${R_REGISTRY}/src/contrib/${archive}"

docs:
    FROM alpine/curl:8.20.0
    WORKDIR /app
    COPY +build/generated generated

    # Documentation for python client
    WORKDIR /app/generated/python
    ARG --required RAW_REGISTRY_SNAPSHOTS
    ARG --required OPENAPI_VERSION
    RUN \
        --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            export DOC_ARCHIVE=odm-api-python-${OPENAPI_VERSION}.tar.gz && \
            tar cf ${DOC_ARCHIVE} README.md docs/* && \
            curl -v --fail --user ${NEXUS_USER}:${NEXUS_PASSWORD} \
                -H 'Content-Type: application/gzip' \
                 --upload-file ${DOC_ARCHIVE} \
                 ${RAW_REGISTRY_SNAPSHOTS}/docs/odm-api-python/${DOC_ARCHIVE}

    # Documentation for r client
    WORKDIR /app/generated/r
    RUN \
        --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            export DOC_ARCHIVE=odm-api-r-${OPENAPI_VERSION}.tar.gz && \
            tar cf ${DOC_ARCHIVE} README.md docs/* && \
            curl -v --fail --user ${NEXUS_USER}:${NEXUS_PASSWORD} \
                -H 'Content-Type: application/gzip' \
                 --upload-file ${DOC_ARCHIVE} \
                 ${RAW_REGISTRY_SNAPSHOTS}/docs/odm-api-r/${DOC_ARCHIVE}

swagger:
    FROM swaggerapi/swagger-ui:v5.32.6

    COPY +build/v1 /usr/share/nginx/html/yaml/
    COPY openapi/swagger/fs /

    RUN rm -f /usr/share/nginx/html/yaml/odmApi.yaml
    RUN apk add bash --no-cache && \
        rewrite_entrypoint.sh && \
        apk del bash && \
        echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories && \
        apk update && apk upgrade sqlite-libs

    # Remove merged api spec
    # IDK why it's required
    RUN ln -s /usr/share/nginx/html/yaml /usr/share/nginx/html/helper/yaml

    ENTRYPOINT ["/genestack-docker-entrypoint.sh"]
    CMD ["nginx", "-g", "daemon off;"]

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

stoplight:
    FROM nginxinc/nginx-unprivileged:1.31.2-alpine

    COPY +build/v1/schemas /usr/share/nginx/html/schemas/
    COPY +build/v1/odmApi.yaml /usr/share/nginx/html/
    COPY openapi/stoplight/fs /

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/stoplight:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/stoplight:latest

main:
    BUILD +swagger
    BUILD +stoplight
    BUILD +docs
    BUILD +r-api-client
    BUILD +python-api-client
