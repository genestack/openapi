#!/bin/bash

set -eu

# Push python SDK's
for directory in $(find generated/python -maxdepth 1 -type d -not -path generated/python); do
    cd "$directory"
    python3 setup.py sdist
        if echo ${ODM_OPENAPI_VERSION} | grep -Exq "^([a-zA-Z0-9]+(.)?){3}$"; then
          twine upload -r nexus-pypi-releases dist/*
        else
          twine upload -r nexus-pypi-snapshots dist/*
        fi
    cd "$OLDPWD"
done
