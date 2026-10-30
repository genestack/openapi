# Build and delivery

This guide governs the build, dependency, CI, and documentation asset paths
assigned in the [root instructions](../../AGENTS.md#scope-and-reading-routes).
It also applies when validating generation or packaging after an API edit.

## Inputs and generation

[build.gradle.kts](../../build.gradle.kts) defines SDK generation;
[MergeSpecifications.merge](../../buildSrc/src/main/kotlin/com/genestack/openapi/MergeSpecifications.kt)
implements the merge. Top-level `openapi/v1/*.yaml` inputs are sorted, the output
file is excluded, and Jackson updates them in sequence. Object members merge
recursively; later scalar values replace earlier ones, including conflicting
`info` fields. Arrays such as `tags` and `security` append without deduplication.
Duplicate path/method objects combine silently, including their arrays, rather
than causing a collision error. Keep shared definitions consistent instead of
relying on merge order. Schemas remain external references, so the output YAML
and `schemas/` tree belong together.

[Earthfile](../../Earthfile) adds `processorsController.yaml` from the
`odm-oas/processors-controller` image before generation. Local Gradle only sees
files already present. A successful local run without that supplied fragment
does not validate the complete packaged SDK or documentation. Use the requested
processors-controller version for full packaging; do not invent its contents.

- `OPENAPI_VERSION` is required during Gradle configuration, even for tasks such
  as `help` or `mergeSpecifications`. It sets SDK package versions separately from
  the API fragments' `info.version`.
- `generateOdmApiPython` writes `generated/python` with package `odm_api`;
  `generateOdmApiR` writes `generated/r` with package `odmApi`. Both depend on
  `mergeSpecifications`. Generator and wrapper versions live in
  [libs.versions.toml](../../gradle/libs.versions.toml) and
  [gradle-wrapper.properties](../../gradle/wrapper/gradle-wrapper.properties).
- `generateAll` deletes the entire `generated/` tree in its task configuration
  block, then invokes both generators through `GradleBuild`. Even a dry run that
  configures this task can delete output. Preserve needed local artifacts first.
  Use `mergeSpecifications` when only the merged input needs checking.
- Keep the accession name mapping aligned across both generators. Python's
  `poetry1` option has a documented upstream workaround in the build file, and
  `mapNumberTo = float` supports non-finite numeric values. Evaluate their client
  effects before removing or changing them. Commented configuration options
  are inactive.

## Documentation consumers

[Earthfile](../../Earthfile) assembles both documentation images from build output:

| Consumer | Input and behavior |
| --- | --- |
| Swagger | Copies the full `v1/` tree into `/usr/share/nginx/html/yaml/`, removes `odmApi.yaml` and the internal `AGENTS.md` guide, and lists the remaining top-level YAML fragments |
| Stoplight | Copies `odmApi.yaml` and `schemas/` together; the [HTML component](../../openapi/stoplight/fs/usr/share/nginx/html/index.html) requests `/stoplight/odmApi.yaml` |

The Swagger [rewrite script](../../openapi/swagger/fs/usr/sbin/rewrite_entrypoint.sh)
runs during image construction and writes `/genestack-docker-entrypoint.sh`.
That generated script expands `SWAGGER_FE_REMOTE_URL` at container startup and
delegates to the upstream `/docker-entrypoint.sh`. The rewrite script needs Bash;
its output must run with `/bin/sh`, since the image removes Bash afterward.
Do not run the rewrite script directly on the host: its paths target the container.

Swagger copies the whole `v1/` tree, including schema subdirectories. When adding
an internal guide under that tree, account for the copy and keep it out of the
served image; the existing Earthfile exclusion covers `v1/AGENTS.md` only.

Fragment basenames become Swagger selector names. When adding or renaming a
fragment, review the links in the [helper page](../../openapi/swagger/fs/usr/share/nginx/html/helper/index.html)
and preserve relative schema resolution. The helper's `/swagger/` links, the
generated `URLS` entries at `$SWAGGER_FE_REMOTE_URL/swagger/yaml/...`, and
Stoplight's `/stoplight/` URL assume deployment prefixes. The
[Stoplight nginx config](../../openapi/stoplight/fs/etc/nginx/conf.d/default.conf)
serves files at its own root; verify deployment routing when changing those URLs.

## Validation

Use the repository's Gradle wrapper. The maintained container build and
[CI setup](../../.github/workflows/pre-commit.yml) specify Java 25; check the local
JDK against the configured wrapper before diagnosing build failures. Dependencies
may need network access. No tracked Gradle unit tests cover the custom merger.
For merger or Jackson changes, check small inputs with overlapping object
members, conflicting scalars, and arrays as well as real fragments. A successful
parse or generation does not establish the intended collision behavior.

With `OPENAPI_VERSION` supplied, run `./gradlew mergeSpecifications` to inspect a
merge, or `./gradlew generateAll` when both SDKs need regeneration and replacing
their output is acceptable. Check changed operations, models, wire names, and
external references in the resulting artifacts. Treat pre-existing ignored output
as evidence only after confirming it reflects the current inputs and generator.

[Earthfile](../../Earthfile) defines the SDK checks in their generated directories:

- In `generated/python`, `python3 -m tox run-parallel` followed by
  `python3 setup.py sdist`. Tool dependencies are in
  [requirements.txt](../../requirements.txt); the generated package supplies its
  own runtime and test dependencies.
- In `generated/r`, `R CMD build .` followed by `R CMD check *.tar.gz --no-manual`.
  [requirements.R](../../requirements.R) installs the build's R
  dependencies. Read it before running: it installs packages and uses the network.

Generated test skeletons are not server integration coverage. CI's tracked
pre-commit job runs the configured hooks, not these generation/package checks.
Report partial local input coverage, unavailable toolchains, or skipped SDK checks
without treating them as passes.

Earthly's `main` combines SDK and documentation targets. SDK targets contain
`RUN --push` uploads to package registries; documentation targets contain
`SAVE IMAGE --push` for versioned and `latest` tags. Read their arguments and
secrets before invoking them. Local validation does not require enabling push
execution. Publishing remains subject to the user's requested scope.
