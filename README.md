# Swagger API specification and documentation

This directory contains source files to generate the R and Python API SDKs.

## Mandatory! Working with the repository: setting up pre-commit
To work with this repository it's **mandatory** to install pre-commit.
[Official documentation](https://pre-commit.com/#installation)

## Building artifacts

```bash
./gradlew generateAll
```

Resulting artifacts are generated in the `generated` directory at the
repository root.

## Docker image

The `Makefile` builds the `Dockerfile`'s targets. Each one takes processors-controller's spec from its `odm-oas` image, so `PROCESSORS_CONTROLLER_VERSION` is required:

```bash
make swagger PROCESSORS_CONTROLLER_VERSION=<version>  # the Swagger UI image CI publishes and compose runs
make test PROCESSORS_CONTROLLER_VERSION=<version>     # generate both clients, then tox run-parallel and R CMD check
```

`make swagger` loads the image into the local Docker. `IMAGE_PUSH=registry` publishes it instead, and `IMAGE_VERSION` sets its tag and both clients' version. It also tags `latest`. `make push` also builds both clients, and with `IMAGE_PUSH` registry or full uploads the Python client to PyPI (TestPyPI for a development version) and the R client to Nexus, with the secrets in `.secret` and the R repositories in `.arg`. `make help` lists every target.
