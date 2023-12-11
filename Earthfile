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
    COPY requirements.R .
    ARG APT_PACKAGES=build-essential \
        pandoc texinfo texlive-latex-extra \
        texlive-fonts-extra libz-dev libxml2-dev \
        libcurl4-openssl-dev libssl-dev libssl-doc \
        automake file libfribidi-dev libgfortran5 \
        libfontconfig1-dev libgdal-dev libzmq3-dev \
        libharfbuzz-dev libfreetype6-dev libpng-dev \
        libtiff5-dev libjpeg-dev libxml2-dev \
        libicu70 libgomp1 libreadline8
    RUN \
        apt update && apt install -y ${APT_PACKAGES} && \
        Rscript requirements.R

    COPY pom.xml requirements.txt .
    COPY .mvn .mvn
    COPY mvnw mvnw
    RUN \
        ./mvnw de.qaware.maven:go-offline-maven-plugin:1.2.8:resolve-dependencies \
            -Drevision=dummyValue && \
        python3 -m pip install -r requirements.txt

    SAVE IMAGE --cache-hint

build:
    FROM +deps

    COPY --dir openapi scripts docs templates .
    # R allows only numeric versions
    # https://r-pkgs.org/lifecycle.html
    ARG --required ODM_OPENAPI_VERSION
    ENV ODM_OPENAPI_VERSION=${ODM_OPENAPI_VERSION}
    RUN ./mvnw package

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

swagger-image:
    FROM openapi+swagger-ui

    ARG --required ODM_OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${ODM_OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

main:
    BUILD +swagger-image
    BUILD +r-api-sdk
    BUILD +python-api-sdk
