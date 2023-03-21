#!/bin/bash

set -eu

# Push python SDK's
for directory in $(find generated/python -maxdepth 1 -type d -not -path generated/python); do
    cd "$directory"
    python2 setup.py sdist
    twine upload -r nexus-pypi-snapshots dist/*
    cd "$OLDPWD"
done

# Build R archives
for directory in $(find generated/r -maxdepth 1 -type d -not -path generated/r); do
    cd "$directory"
    R -e "devtools::build()"
    cd "$OLDPWD"
done

# Push R SDK's
cd generated/r

for archive in $(find -type f -name "*.tar.gz" -printf '%P\n') ; do
    curl --user "${NEXUS_USER}:${NEXUS_PASSWORD}" \
       --upload-file "$archive" "${R_REGISTRY_SNAPSHOTS}/genestack/odm/api-sdk/$archive"
done
