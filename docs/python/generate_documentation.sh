#!/bin/bash

APP_ID="${1?APP_ID needs to be defined}"
PACKAGE_NAME="${2?PACKAGE_NAME needs to be defined}"


SCRIPT_PATH=${0%/*}
GENERATED_CLIENT_PATH=${SCRIPT_PATH}/../../../generated/python/${APP_ID}/${PACKAGE_NAME}


# Copy source/ folder with conf.py, _static/ and _templates/
cp -r ${SCRIPT_PATH}/source ${GENERATED_CLIENT_PATH}/

# Generate `.rst` files automatically using sphinx-apidoc
sphinx-apidoc -o ${GENERATED_CLIENT_PATH}/source/ ${GENERATED_CLIENT_PATH} --no-toc --force
# Remove generated docs for technical modules
rm ${GENERATED_CLIENT_PATH}/source/${PACKAGE_NAME}.rst

# Reformat docstring within python files
python ${GENERATED_CLIENT_PATH}/source/prepare_sphinx_py_files.py --client-folder ${GENERATED_CLIENT_PATH}

# Generate index.rst from README.md file and prettify api and models .rst files
python ${GENERATED_CLIENT_PATH}/source/prepare_sphinx_rst_files.py \
    --package ${2} \
    --readme ${GENERATED_CLIENT_PATH}/../README.md  \
    --sphinx-folder ${GENERATED_CLIENT_PATH}/source

# Run sphinx build
sphinx-build -W -b singlehtml ${GENERATED_CLIENT_PATH}/source/ ${GENERATED_CLIENT_PATH}/../sphinx_docs
