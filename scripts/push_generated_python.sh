#!/bin/bash

set -eu

# Push python SDK's
for directory in $(find generated/python -maxdepth 1 -type d -not -path generated/python); do
    cd "$directory"
    python2 setup.py sdist
    twine upload -r nexus-pypi-snapshots dist/*
    cd "$OLDPWD"
done
