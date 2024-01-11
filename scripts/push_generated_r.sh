#!/bin/bash

set -eu

# Build R archives
for directory in $(find generated/r -maxdepth 1 -type d -not -path generated/r); do
    cd "$directory"
    R CMD build .
    cd "$OLDPWD"
done

# Push R SDK's
cd generated/r


# https://help.sonatype.com/repomanager3/nexus-repository-administration/formats/r-repositories#RRepositories-UploadbyHTTPPOST
for archive in $(find -type f -name "*.tar.gz" -printf '%P\n') ; do
    if echo ${ODM_OPENAPI_VERSION} | grep -Exq "^([a-zA-Z0-9]+(.)?){3}$"; then
      curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
         --upload-file "$archive" "${R_REGISTRY_RELEASES}/src/contrib/$archive"
    else
      curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
         --upload-file "$archive" "${R_REGISTRY_SNAPSHOTS}/src/contrib/$archive"
    fi
done
