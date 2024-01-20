VERSION 0.7

IMPORT ./openapi

ARG --global --required HARBOR_DOCKER_REGISTRY
ARG --global --required MAVEN_REGISTRY_GROUP
ARG --global --required MAVEN_REGISTRY_RELEASES
ARG --global --required MAVEN_REGISTRY_SNAPSHOTS
ARG --global --required NPM_REGISTRY_GROUP
ARG --global --required NPM_REGISTRY_RELEASES
ARG --global --required NPM_REGISTRY_SNAPSHOTS
ARG --global --required PYPI_REGISTRY_GROUP
ARG --global --required PYPI_REGISTRY_RELEASES
ARG --global --required PYPI_REGISTRY_SNAPSHOTS
ARG --global --required R_REGISTRY_GROUP
ARG --global --required R_REGISTRY_RELEASES
ARG --global --required R_REGISTRY_SNAPSHOTS
ARG --global --required NEXUS_REPOSITORY_URL

deps:
    ARG --required BASE_IMAGES_VERSION
    FROM ${HARBOR_DOCKER_REGISTRY}/builder:${BASE_IMAGES_VERSION}
    COPY requirements.R requirements.txt .
    RUN \
        Rscript requirements.R && \
        python3 \
            -m pip install \
            -r requirements.txt

    SAVE IMAGE --cache-hint

build:
    FROM +deps

    COPY --dir openapi scripts gradle gradlew buildSrc build.gradle.kts settings.gradle.kts .
    # R allows only numeric versions
    # https://r-pkgs.org/lifecycle.html
    ARG --required ODM_OPENAPI_VERSION
    ENV ODM_OPENAPI_VERSION=${ODM_OPENAPI_VERSION}
    RUN ./gradlew \
            generateAllApiSdks \
            --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated

r-api-sdk:
    FROM +build
    RUN --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            scripts/push_generated_r.sh

python-api-sdk:
    FROM +build
    RUN --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            pypi-login.sh && \
            scripts/push_generated_python.sh

openapi-definition:
    FROM +build
    ARG OPENAPI_ARCHIVE=definition-${ODM_OPENAPI_VERSION}.tar.gz
    RUN --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            tar cvf ${OPENAPI_ARCHIVE} openapi/v1 && \
            curl -v --fail --user ${NEXUS_USER}:${NEXUS_PASSWORD} \
                -H 'Content-Type: application/gzip' \
                 --upload-file ${OPENAPI_ARCHIVE} \
                 ${RAW_REGISTRY_SNAPSHOTS}/openapi/${OPENAPI_ARCHIVE}

swagger-image:
    FROM openapi+swagger-ui

    ARG --required ODM_OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${ODM_OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

main:
    BUILD +swagger-image
    BUILD +r-api-sdk
    BUILD +python-api-sdk
    BUILD +openapi-definition
