VERSION 0.8

ARG --global --required HARBOR_DOCKER_REGISTRY

build:
    FROM eclipse-temurin:25.0.4_7-jdk-alpine
    WORKDIR /app

    CACHE /root/.gradle/caches
    CACHE /root/.gradle/wrapper

    COPY --dir openapi gradle gradlew buildSrc build.gradle.kts settings.gradle.kts .
    COPY +processors-controller-spec/openapi.yaml openapi/v1/processorsController.yaml

    ARG --required OPENAPI_VERSION
    RUN ./gradlew generateAll --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated
    SAVE ARTIFACT openapi/v1

# processors-controller's ODM OpenAPI spec, from the data image its build pushes
processors-controller-spec:
    ARG --required PROCESSORS_CONTROLLER_VERSION
    FROM ${HARBOR_DOCKER_REGISTRY}/odm-oas/processors-controller:${PROCESSORS_CONTROLLER_VERSION}
    SAVE ARTIFACT /openapi.yaml

python-api-client:
    FROM python:3.15.1-alpine
    WORKDIR /app

    CACHE /root/.cache

    COPY requirements.txt .
    RUN python3 -m pip install -r requirements.txt

    COPY +build/generated generated
    WORKDIR generated/python

    # Test and build python client
    RUN \
        python3 -m tox run-parallel && \
        python3 setup.py sdist

    # Releases go to PyPI, development builds to TestPyPI
    ARG --required OPENAPI_VERSION
    IF echo ${OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        ARG PYPI_REPOSITORY="https://upload.pypi.org/legacy/"
        ARG PYPI_TOKEN_SECRET="PYPI_TOKEN"
    ELSE
        ARG PYPI_REPOSITORY="https://test.pypi.org/legacy/"
        ARG PYPI_TOKEN_SECRET="PYPI_TOKEN_TEST"
    END

    RUN --push \
        --secret PYPI_TOKEN \
        --secret PYPI_TOKEN_TEST \
            TWINE_USERNAME=__token__ TWINE_PASSWORD="pypi-$(printenv ${PYPI_TOKEN_SECRET})" \
                twine upload dist/* --repository-url ${PYPI_REPOSITORY}

r-api-client:
    FROM rocker/r-ver:4.6.1
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

swagger:
    FROM swaggerapi/swagger-ui:v5.34.0

    COPY +build/v1 /usr/share/nginx/html/yaml/
    COPY openapi/swagger/fs /

    RUN rm -f /usr/share/nginx/html/yaml/odmApi.yaml
    RUN apk add bash --no-cache && \
        rewrite_entrypoint.sh && \
        apk del bash && \
        apk upgrade --no-cache --repository http://dl-cdn.alpinelinux.org/alpine/edge/main sqlite-libs

    # Remove merged api spec
    # IDK why it's required
    RUN ln -s /usr/share/nginx/html/yaml /usr/share/nginx/html/helper/yaml

    ENTRYPOINT ["/genestack-docker-entrypoint.sh"]
    CMD ["nginx", "-g", "daemon off;"]

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

stoplight:
    FROM nginxinc/nginx-unprivileged:1.31.7-alpine

    COPY +build/v1/schemas /usr/share/nginx/html/schemas/
    COPY +build/v1/odmApi.yaml /usr/share/nginx/html/
    COPY openapi/stoplight/fs /

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/stoplight:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/stoplight:latest

main:
    BUILD +swagger
    BUILD +stoplight
    BUILD +r-api-client
    BUILD +python-api-client
