# Setting SHELL to bash allows bash commands to be executed by recipes.
# Options are set to exit when a recipe line exits non-zero or a piped command fails.
SHELL = /usr/bin/env bash -o pipefail
.SHELLFLAGS = -ec

IMAGE_REGISTRY         ?= harbor.devops.gs.team
IMAGE_NAME             ?= genestack/swagger
IMAGE_VERSION          ?= 1.0.0
IMAGE_TAG              := $(IMAGE_REGISTRY)/$(IMAGE_NAME):$(IMAGE_VERSION)
IMAGE_TAG_LATEST       := $(IMAGE_REGISTRY)/$(IMAGE_NAME):latest
IMAGE_PUSH             ?= docker
IMAGE_DEFAULT_PLATFORM := $(shell docker system info -f '{{.OSType}}/{{.Architecture}}')
IMAGE_PLATFORMS        ?= $(IMAGE_DEFAULT_PLATFORM)

ifeq ($(IMAGE_PUSH),full)
	IMAGE_BUILD_OUTPUTS := --output=type=docker --output=type=registry
else ifeq ($(IMAGE_PUSH),docker)
	IMAGE_BUILD_OUTPUTS := --output=type=docker
else ifeq ($(IMAGE_PUSH),registry)
	IMAGE_BUILD_OUTPUTS := --output=type=registry
else ifeq ($(IMAGE_PUSH),none)
	IMAGE_BUILD_OUTPUTS :=
endif

# Every target generates the artifacts from openapi/v1 and processors-controller's spec,
# which processors-controller's build pushes as the odm-oas data image. IMAGE_VERSION is
# the version of the image and both clients; Gradle reads it as OPENAPI_VERSION.
BUILD_INPUTS = \
	--build-context=processors_controller_spec=docker-image://$(IMAGE_REGISTRY)/genestack/odm-oas/processors-controller:$(or $(PROCESSORS_CONTROLLER_VERSION),$(error PROCESSORS_CONTROLLER_VERSION is not set)) \
	--build-arg=OPENAPI_VERSION=$(IMAGE_VERSION)

# Uploads run only when IMAGE_PUSH sends images to the registry (registry, full), as CI's Push
# build sets it. A local `make push` builds the artifacts and uploads nothing.
UPLOAD                 := $(filter registry full,$(IMAGE_PUSH))

.PHONY: main
main: push

##@ General

# The help target prints out all targets with their descriptions organized
# beneath their categories. The categories are represented by '##@' and the
# target descriptions by '##'. The awk command is responsible for reading the
# entire set of makefiles included in this invocation, looking for lines of the
# file as xyz: ## something, and then pretty-format the target and help. Then,
# if there's a line with ##@ something, that gets pretty-printed as a category.
# More info on the usage of ANSI control characters for terminal formatting:
# https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
# More info on the awk command:
# http://linuxcommand.org/lc3_adv_awk.php

.PHONY: help
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Build

.PHONY: push
push: swagger python-client r-client ## Build the image and both clients, without checks or tests; publish and upload them with IMAGE_PUSH registry or full.

.PHONY: swagger
swagger: ## Build the Swagger UI image, tagged IMAGE_VERSION and latest, to IMAGE_PUSH (docker, registry, full, none).
	docker buildx build \
	--platform=$(IMAGE_PLATFORMS) \
	--target=swagger \
	$(BUILD_INPUTS) \
	--tag=$(IMAGE_TAG) \
	--tag=$(IMAGE_TAG_LATEST) \
	$(IMAGE_BUILD_OUTPUTS) \
	.

# In CI the Vault step writes every secret to .secret and the R repositories to .arg in
# the checkout (teamcity-main's templates/Vault.kt).
.PHONY: python-client
python-client: ## Build the Python client and, with IMAGE_PUSH registry or full, upload it to PyPI (release IMAGE_VERSION) or TestPyPI, with PYPI_TOKEN and PYPI_TOKEN_TEST from .secret or the environment.
	@if [ -f .secret ]; then set -a; . ./.secret; set +a; fi; \
	docker buildx build \
	--target=$(if $(UPLOAD),python-client-upload,python-client) \
	$(BUILD_INPUTS) \
	--secret=id=PYPI_TOKEN,env=PYPI_TOKEN \
	--secret=id=PYPI_TOKEN_TEST,env=PYPI_TOKEN_TEST \
	--no-cache-filter=python-client-upload \
	--output=type=cacheonly \
	.

.PHONY: r-client
r-client: ## Build the R client and, with IMAGE_PUSH registry or full, upload it to R_REGISTRY_RELEASES (release IMAGE_VERSION) or R_REGISTRY_SNAPSHOTS from .arg, with NEXUS_USER and NEXUS_PASSWORD from .secret.
	@if [ -f .secret ]; then set -a; . ./.secret; set +a; fi; \
	if [ -f .arg ]; then set -a; . ./.arg; set +a; fi; \
	docker buildx build \
	--target=$(if $(UPLOAD),r-client-upload,r-client) \
	$(BUILD_INPUTS) \
	--build-arg=R_REGISTRY_RELEASES \
	--build-arg=R_REGISTRY_SNAPSHOTS \
	--secret=id=NEXUS_USER,env=NEXUS_USER \
	--secret=id=NEXUS_PASSWORD,env=NEXUS_PASSWORD \
	--no-cache-filter=r-client-upload \
	--output=type=cacheonly \
	.

.PHONY: test
test: ## Generate both clients, then run tox run-parallel on the Python one and R CMD check on the R one.
	docker buildx build \
	--target=test \
	$(BUILD_INPUTS) \
	--output=type=cacheonly \
	.
