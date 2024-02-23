VERSION 0.8

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

build:
    ARG --required BASE_IMAGES_VERSION
    FROM ${HARBOR_DOCKER_REGISTRY}/builder:${BASE_IMAGES_VERSION}

    CACHE /root/.gradle/caches
    CACHE /root/.gradle/wrapper
    CACHE /root/.cache

    COPY requirements.R requirements.txt .
    COPY --dir openapi scripts gradle gradlew build.gradle.kts settings.gradle.kts .
    COPY --dir buildSrc/src buildSrc/build.gradle.kts buildSrc/settings.gradle.kts buildSrc/.

    RUN Rscript requirements.R && \
        python3 \
            -m pip install \
            -r requirements.txt

    ARG --required ODM_OPENAPI_VERSION
    ENV ODM_OPENAPI_VERSION=${ODM_OPENAPI_VERSION}
    RUN ./gradlew \
            generateAll \
            --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated

build-clients:
    FROM +build

    # Build python client
    RUN \
        cd generated/python/odm-api && \
        python3 setup.py sdist

    # Build r client
    RUN \
        cd generated/r/odm-api && \
        R CMD build .

    SAVE IMAGE --cache-hint

python-api-client:
    FROM +build-clients

    IF echo ${ODM_OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        RUN --push \
            --secret PYPI_TOKEN \
            --secret PYPI_TOKEN_TEST \
                pypi-login.sh && \
                twine upload dist/* -r testpypi && \
                twine upload dist/* && \
                pypi-clean.sh
    ELSE
        RUN --push \
            --secret NEXUS_USER \
            --secret NEXUS_PASSWORD \
                pypi-login.sh && \
                twine upload dist/* -r nexus-pypi-snapshots && \
                pypi-clean.sh
    END

r-api-client:
    FROM +build-clients

    IF echo ${ODM_OPENAPI_VERSION} | grep -Exq "^([0-9]+(.)?){3}$"
        RUN --push \
            --secret NEXUS_USER \
            --secret NEXUS_PASSWORD \
               cd generated/r/odm-api && \
               export archive=$(find . | grep tar.gz | sed 's|./||') && \
               curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
                  --upload-file "${archive}" "${R_REGISTRY_RELEASES}/src/contrib/${archive}"
    ELSE
        RUN --push \
            --secret NEXUS_USER \
            --secret NEXUS_PASSWORD \
               cd generated/r/odm-api && \
               export archive=$(find . | grep tar.gz | sed 's|./||') && \
               curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
                  --upload-file "${archive}" "${R_REGISTRY_RELEASES}/src/contrib/${archive}"
    END

swagger-image:
    FROM openapi+swagger-ui

    ARG --required ODM_OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:${ODM_OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/swagger:latest

main:
    BUILD +swagger-image
    BUILD +r-api-client
    BUILD +python-api-client
