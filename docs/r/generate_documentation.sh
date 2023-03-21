#!/bin/bash

SCRIPT_PATH=${0%/*}

# Set CLIENT_PATH bash variable to the R package actual path
CLIENT_PATH=${SCRIPT_PATH}/../../../generated/r/${1}/

# Reformat roxygen strings within sources
python ${SCRIPT_PATH}/prepare_roxygen_r_files.py --source-folder ${CLIENT_PATH}/R/

# Create .Rd files from sources
R -e "devtools::document('${CLIENT_PATH}')"

# Build .pdf manual
R CMD Rd2pdf ${CLIENT_PATH} -o ${CLIENT_PATH}/manual.pdf --no-index --no-preview --force
