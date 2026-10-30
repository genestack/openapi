# Repository instructions

This repository owns ODM OpenAPI descriptions, generates Python and R SDKs,
and packages Swagger UI and Stoplight documentation. It contains no ODM server
implementation. A schema edit changes the published API description and generated
clients; it does not establish or change server behavior.

## Scope and reading routes

Read each `AGENTS.md` from the repository root down to the affected files before
planning, editing, or reviewing an area. When starting at the root, open the area
guides explicitly. More specific guides specialize their ancestors and cannot
relax root instructions or shared conventions. Links to other areas
supply context unless shared authority is explicitly assigned below.
Platform instructions, user requests, and tool permissions take precedence over
repository guidance.

| Task | Governing guidance and entry points |
| --- | --- |
| Endpoints, models, API examples, compatibility review | [API guide](openapi/v1/AGENTS.md), then the affected fragment and its schema references |
| Merging, SDK generation, dependencies, containers, documentation assets | [Build and delivery guide](docs/agents/build-and-delivery.md) |
| Agent documentation changes | Keep these reading routes and source links accurate; validate links and whitespace without regenerating SDKs |

Codex and supported Claude Code installations use this same rule set. When
changing agent entry points or troubleshooting loading, read the
[loading note](docs/agents/entry-points.md).

The build and delivery guide has shared authority over `build.gradle.kts`,
`settings.gradle.kts`, `buildSrc/**`, `gradle/**`, `gradlew`, `gradlew.bat`,
`Earthfile`, `requirements.txt`, `requirements.R`, `renovate.json`,
`.github/workflows/**`, and the `openapi/swagger/**` and `openapi/stoplight/**`
assets. Apply it as well when an API change needs generation or packaging checks.

## Shared conventions

- Edit tracked sources. [Generation configuration](build.gradle.kts) and
  [.gitignore](.gitignore) identify disposable `generated/`, `build/`,
  `openapi/v1/odmApi.yaml`, and the externally supplied
  `openapi/v1/processorsController.yaml`. Existing ignored output may contain
  local work or be stale. Inspect before replacing it; do not hand-edit it as
  the durable fix.
- Keep public wire names, operation IDs, tags, requiredness, security, and media
  types compatible unless the requested change includes changing that contract.
  API changes must account for both generated languages and both documentation
  consumers. The guides identify their different inputs.
- Keep unrelated cleanup and dependency upgrades outside the task.
- Verify changed API semantics against the server implementation outside this
  checkout, or flag the change as unverified. Generation and local mocks do not
  establish server acceptance. For live checks, use only a target supplied or
  explicitly approved by the user for this task; continue independent local
  checks when no target is available.
- [README.md](README.md#mandatory-working-with-the-repository-setting-up-pre-commit)
  requires pre-commit installation. Use the checks in
  [.pre-commit-config.yaml](.pre-commit-config.yaml), including new files:
  `pre-commit run --files <changed-paths>`. Its YAML hook checks syntax with
  `--unsafe`; it does not validate OpenAPI semantics or resolve references.
- [.github/CODEOWNERS](.github/CODEOWNERS) assigns the repository to DevOps and
  ODM backend and explicitly says Terraform manages that file. Change ownership
  through its managing source rather than editing it here.

## Completion evidence

Report the changed contracts, checks actually run, and any unverified behavior.
For API or build changes, use the relevant guide's validation evidence. Prose-only
changes need the configured hooks and `git diff --check`, with intended new files
included in the hook invocation; application generation is unnecessary. Check
local Markdown links when adding or changing guidance.
