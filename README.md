# Swagger API specification and documentation

This directory contains source files to generate the following artifacts:
- R and Python API SDK
- TODO: Postman collection

## Building artifacts

```bash
./gradlew generateAllApiSdks
```

Resulting artifacts are generated in the `generated` directory at the
repository root.

## Specification structure

* For keeping consistency we have to use `components.schemas` in all files, it affects result classname in API SDK.
