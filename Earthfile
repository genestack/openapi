VERSION 0.7

IMPORT ./openapi

ARG --global --required HARBOR_DOCKER_SNAPSHOTS
ARG --global --required HARBOR_DOCKER_RELEASES
ARG --global --required HARBOR_DOCKER_CACHE
ARG --global --required HARBOR_DOCKER_HUB_MIRROR
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
    FROM ${HARBOR_DOCKER_SNAPSHOTS}/genestack-builder:${BASE_IMAGES_VERSION}
    COPY --dir pom.xml deps .

    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            mvn de.qaware.maven:go-offline-maven-plugin:1.2.8:resolve-dependencies -T 1C \
                -Drevision=dummyValue && \
            python2 -m pip install -r deps/python/python2-requirements.txt && \
            python3 -m pip install -r deps/python/python3-requirements.txt && \
            deps/r/ubuntu.sh

build:
    FROM +deps

    COPY --dir openapi scripts docs templates .
    # R allows only numeric versions
    # https://r-pkgs.org/lifecycle.html
    ARG --required ODM_OPENAPI_VERSION
    ENV ODM_OPENAPI_VERSION=${ODM_OPENAPI_VERSION}
    RUN mvn package -T 1C

    SAVE IMAGE --cache-hint

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

swagger-ui-image:
    FROM openapi+swagger-ui \
         --HARBOR_DOCKER_HUB_MIRROR=${HARBOR_DOCKER_HUB_MIRROR}

    ARG --required ODM_OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_SNAPSHOTS}/genestack-swagger-ui:${ODM_OPENAPI_VERSION}

main:
    BUILD +swagger-ui-image
    BUILD +r-api-sdk
    BUILD +python-api-sdk
