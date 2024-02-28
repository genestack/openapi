VERSION --pass-args 0.7

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
ARG --global --required NEXUS_URL

deps:
    ARG --required BASE_IMAGES_VERSION
    FROM ${HARBOR_DOCKER_REGISTRY}/builder:${BASE_IMAGES_VERSION}

    CACHE /root/.cache

    COPY requirements.R requirements.txt .

    # Gcc and other stuff for R source packages building
    RUN \
        apt update && \
        apt install -y build-essential libssl-dev libcurl4-openssl-dev

    # Install dependencies for R and Python
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            Rscript requirements.R && \
            pypi-login.sh && \
            python3 \
                -m pip install \
                -r requirements.txt && \
            pypi-clean.sh

    SAVE IMAGE --cache-hint

build:
    FROM +deps

    CACHE /root/.gradle/caches
    CACHE /root/.gradle/wrapper

    COPY --dir openapi gradle gradlew build.gradle.kts settings.gradle.kts .
    COPY --dir buildSrc/src buildSrc/build.gradle.kts buildSrc/settings.gradle.kts buildSrc/.

    ARG --required OPENAPI_VERSION
    ENV OPENAPI_VERSION=${OPENAPI_VERSION}
    RUN ./gradlew \
            generateAll \
            --no-daemon

    SAVE IMAGE --cache-hint
    SAVE ARTIFACT generated

python-api-client:
    FROM +build
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
    FROM +build
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

mkdocs:
    ARG --required BASE_IMAGES_VERSION
    FROM ${HARBOR_DOCKER_REGISTRY}/python3:${BASE_IMAGES_VERSION}

    COPY mkdocs/fs /
    RUN \
        --secret NEXUS_USER \
        --secret NEXUS_PASSWORD \
            pypi-login.sh && \
            python3 \
                -m pip install \
                -r requirements.txt && \
            pypi-clean.sh

    COPY +build/generated /app/docs/generated/
    ENTRYPOINT ["mkdocs", "serve"]

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/mkdocs:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/mkdocs:latest


explorer:
    ARG --required BASE_IMAGES_VERSION
    FROM --pass-args openapi+explorer

    ARG --required OPENAPI_VERSION
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/explorer:${OPENAPI_VERSION}
    SAVE IMAGE --push ${HARBOR_DOCKER_REGISTRY}/explorer:latest


main:
    BUILD +swagger
    BUILD +explorer
    BUILD +mkdocs
    BUILD +r-api-client
    BUILD +python-api-client
