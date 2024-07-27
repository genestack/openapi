VERSION 0.8

IMPORT ./openapi

ARG --global --required HARBOR_DOCKER_REGISTRY
ARG --global --required MAVEN_REGISTRY_GROUP
ARG --global --required MAVEN_REGISTRY_RELEASES
ARG --global --required MAVEN_REGISTRY_SNAPSHOTS

build:
    FROM eclipse-temurin:21.0.4_7-jdk-alpine
    DO github.com/genestack/earthly-libs+GRADLE_PREPARE

    CACHE /root/.gradle/caches
    CACHE /root/.gradle/wrapper

    COPY --dir openapi gradle gradlew build.gradle.kts settings.gradle.kts .
    COPY --dir buildSrc/src buildSrc/build.gradle.kts buildSrc/settings.gradle.kts buildSrc/.

    ARG --required OPENAPI_VERSION
    ENV OPENAPI_VERSION=${OPENAPI_VERSION}
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            ./gradlew \
                generateAll \
                --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated

python-api-client:
    FROM python:3.12.4-alpine
    DO github.com/genestack/earthly-libs+PYTHON_PREPARE

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

    IF echo ${OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        ARG PYPI_REPOSITORY_INTERNAL="nexus-pypi-releases"
        ARG PYPI_REPOSITORY_PUBLIC="pypi"
    ELSE
        ARG PYPI_REPOSITORY_INTERNAL="nexus-pypi-snapshots"
        ARG PYPI_REPOSITORY_PUBLIC="testpypi"
    END

    # Push python client
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
    FROM r-base:4.4.1
    WORKDIR /app

    CACHE /root/.cache

    COPY requirements.R .

    # Gcc and other stuff for R source packages building
    RUN \
        apt update && \
        apt install -y build-essential libssl-dev libcurl4-openssl-dev && \
        Rscript requirements.R

    COPY +build/generated generated
    WORKDIR generated/r

    # Test and build R client
    RUN \
        R CMD build . && \
        R CMD check *.tar.gz --no-manual

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
    FROM openapi+swagger

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

explorer:
    FROM --pass-args openapi+explorer

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/explorer:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/explorer:latest

docs:
    FROM alpine/curl:8.9.0
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

main:
    BUILD +swagger
    BUILD +explorer
    BUILD +docs
    BUILD +python-api-client
    # Require a fix for this bug to proceed with using R API CLient:
    # https://github.com/OpenAPITools/openapi-generator/issues/18016
    # BUILD +r-api-client
