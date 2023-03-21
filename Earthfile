VERSION 0.7

IMPORT ./applications/openapi

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


client-api-sdk:
    FROM +deps

    COPY  . .
    # R allows only numeric versions
    # https://r-pkgs.org/lifecycle.html
    ARG --required ODM_OPENAPI_VERSION
    ENV ODM_OPENAPI_VERSION=${ODM_OPENAPI_VERSION}

    # TODO: after moving sdk in separate git repository - move R deps for build to pkg/r
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            mvn install -T 1C -am -pl applications -DskipTests && \
            python2 -m pip install -r client-generator/docs/python/requirements.txt && \
            python3 -m pip install -r client-generator/pkg/python/requirements.txt && \
            mvn package -T 1C -am -pl client-generator -DskipTests -Dclient.generator=default

    RUN --push \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            pypi-login.sh && \
            cd applications && \
            ./push_generated.sh

    SAVE IMAGE --cache-hint


main:
    BUILD +swagger-ui-image
    BUILD +client-api-sdk
