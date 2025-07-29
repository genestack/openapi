# Swagger API specification and documentation

This directory contains source files to generate the following artifacts:
- R and Python API SDK
- Postman collection

## Mandatory! Working with the repository: setting up pre-commit
To work with this repository it's **mandatory** to install pre-commit.
[Official documentation](https://pre-commit.com/#installation)

## Building artifacts

```bash
./gradlew generateAll
```

Resulting artifacts are generated in the `generated` directory at the
repository root.
