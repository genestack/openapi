#!/bin/bash

# This script generates 'startup.sh' script for the swagger container.
#
# The generated script sets URLS environment variable, and then
# calls '/usr/share/nginx/run.sh' to start the swagger server.
#
# We expect container's '$SWAGGER_FE_REMOTE_URL' to contain frontend URL

set -ue

YAML_SPEC_DIR="/usr/share/nginx/html/yaml/"
OUTPUT_FILE="/genestack-docker-entrypoint.sh"

log_err() {
    echo "$@" > /dev/stderr
}

if [[ ! -d "$YAML_SPEC_DIR" ]]; then
    log_err "Error: yaml specs directory not found: $YAML_SPEC_DIR"
    exit 1
fi

generate_swagger_env() {
    # make glob match in a null string if no yaml files found
    shopt -s nullglob
    local YAMLS=("$YAML_SPEC_DIR"/*.yaml)
    shopt -u nullglob

    echo -n 'export URLS="['

    if [[ "${#YAMLS[@]}" -gt 0 ]] ; then
        for yaml_file in "${YAMLS[@]}"; do
            yaml_file_short="$(basename "$yaml_file")"
            # strip extension
            service_name="${yaml_file_short%.*}"
            echo -n " { url: '\$SWAGGER_FE_REMOTE_URL/swagger/yaml/$yaml_file_short', name: '$service_name' },"
        done
    fi

    echo ']"'

    if [[ "${#YAMLS[@]}" -eq 0 ]] ; then
        # place comment in startup file if no .yaml files found
        echo '# warning: no .yaml files found in generation script'
    fi

    echo 'export QUERY_CONFIG_ENABLED=true'

}

{
    # swagger container does not have 'bash', only 'sh'
    echo '#!/bin/sh'
    echo 'set -ex'

    generate_swagger_env
    echo '/docker-entrypoint.sh "$@"'

} > "$OUTPUT_FILE"

sed -i -e "s/,]/]/" "$OUTPUT_FILE"
chmod +x "$OUTPUT_FILE"
