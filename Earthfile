VERSION 0.8

IMPORT ./openapi

ARG --global --required HARBOR_DOCKER_REGISTRY
ARG --global --required MAVEN_REGISTRY_GROUP
ARG --global --required MAVEN_REGISTRY_RELEASES
ARG --global --required MAVEN_REGISTRY_SNAPSHOTS

build:
    FROM eclipse-temurin:21.0.7_6-jdk-alpine
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
    FROM python:3.13.5-alpine
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
    FROM alpine/curl:8.14.1
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

main:
    BUILD +swagger
    BUILD +explorer
    BUILD +docs
    BUILD +python-api-client
