# Swagger API specification and documentation

This directory contains source files to generate the following artifacts:
- R and Python API SDK
- TODO: Postman collection

## Mandatory! Working with the repository: setting up pre-commit
To work with this repository it's **mandatory** to install pre-commit.
[Official documentation](https://pre-commit.com/#installation)

## Building artifacts

```bash
./gradlew generateAll
```

Resulting artifacts are generated in the `generated` directory at the
repository root.

## Specification structure

* For keeping consistency we have to use `components.schemas` in all files, it affects result classname in API SDK.
* File with location `openapi/v1/odmApi.yaml` is autogenerated from all openapi groups in directory `openapi/v1/`,
  it shouldn't be manually changed. Also, the generation of this file is included in the pre-commit. You can manually regenerate it from scratch by:
    ```shell
    ./gradlew mergeDefinitions
    ```
