# API descriptions and schemas

This guide governs `openapi/v1/**` alongside the root instructions. Top-level
YAML files describe endpoint families. `schemas/**` holds shared model sources.
The build merges the fragments for SDKs and Stoplight, while Swagger serves
individual fragments. Keep each fragment's references usable on its own.

## Find the contract

- `*User.yaml` and `*Curator.yaml` describe their respective access modes.
  Compare the matching operations when a change affects both modes. Do not copy
  a whole file across modes: available operations differ. For example,
  [studyCurator.yaml](studyCurator.yaml) has
  `PATCH /api/v1/as-curator/studies/{id}`; [studyUser.yaml](studyUser.yaml) has
  no corresponding PATCH operation.
- `integrationUser.yaml` and `integrationCurator.yaml` own cross-object linkage,
  omics queries, validation, and cell analytics. Entity fragments such as
  `studyUser.yaml` own direct object retrieval and version access. Choose by the
  actual route and operation, not just the model name.
- [job.yaml](job.yaml) owns imports and job execution. [tasks.yaml](tasks.yaml)
  owns task lookup. [scimUsers.yaml](scimUsers.yaml) and
  [scimGroups.yaml](scimGroups.yaml) own account provisioning.
  [manageData.yaml](manageData.yaml) owns deletion and ownership recovery.
  `audit.yaml`, `referenceGenome.yaml`, and `xrefsets.yaml` describe separate APIs.
  The processors-controller contract comes from an external image, as described
  in the [build guide](../../docs/agents/build-and-delivery.md#inputs-and-generation).

## Preserve references and client interfaces

- Before adding a model or fragment, search existing operations and
  `schemas/<area>/*.yaml`. Reuse a model when its wire shape, required properties,
  read-only fields, and composition match the endpoint. Inspect its callers
  before changing it. Some models declare only `type: object`,
  including [Study](schemas/study/Study.yaml) and
  [MetadataContent](schemas/common/MetadataContent.yaml); do not infer a closed
  structure from one example.
- Follow [studyUser.yaml](studyUser.yaml) for fragment-local aliases under
  `components.schemas` referencing `./schemas/...`. External schemas can themselves
  use `#/components/schemas/...` and define sibling aliases, as
  [ListResponse.yaml](schemas/common/ListResponse.yaml) does. Resolve relative
  references from the file containing them, including these nested dependencies.
- Keep shared component names mapped consistently across fragments. Check new
  operation IDs for uniqueness across all fragments and avoid conflicting
  method/path definitions. The [merge behavior](../../docs/agents/build-and-delivery.md#inputs-and-generation)
  does not reject collisions.
- Operation IDs and tags affect generated methods and API groupings. Preserve
  property spelling on the wire, including `genestack:accession` in
  [AccessionResponse.yaml](schemas/common/AccessionResponse.yaml). The generator
  maps that property to `genestackaccession` in SDKs; changing the wire name is
  not a fix for a client identifier problem.
- Keep request `required` lists, `readOnly`, formats, enums, `allOf`, `oneOf`, and
  free-form maps faithful to the endpoint. For example,
  [ExpressionItem.yaml](schemas/expression/ExpressionItem.yaml) permits string
  values as well as numbers inside `value`; generic numeric cleanup could remove
  supported data. Examples alone do not establish requiredness or defaults.

## Metadata and query encoding

Metadata PATCH operations such as `updateStudyAsCurator` in
[studyCurator.yaml](studyCurator.yaml) describe a map of attributes, with explicit
`null` removing an attribute. Preserve that meaning in examples and generated
serialization. SCIM PATCH uses an `Operations` list instead; inspect
[UserPatch.yaml](schemas/scim/UserPatch.yaml),
[GroupPatch.yaml](schemas/scim/GroupPatch.yaml), and the operation descriptions
before reusing a PATCH model across these APIs.

Query parameter names, locations, `style`, and `explode` determine the wire
request. For example, `featureList` on the streamed expression operation in
[integrationUser.yaml](integrationUser.yaml) uses `style: form` with
`explode: true`; deletion `accessions` in [manageData.yaml](manageData.yaml) uses
`explode: false`. Preserve repeated versus comma-separated values and inspect
both SDKs' request serialization when changing these declarations. Query-language
examples also carry quoting and escaping rules; check the affected parameter's
description rather than applying one syntax to every query/filter.

## Similar names with different behavior

- Pagination is endpoint-specific. [ListResponse](schemas/common/ListResponse.yaml)
  uses `meta.pagination`; [ExpressionResponse](schemas/expression/ExpressionResponse.yaml)
  uses `cursor`; [OmicsResponse](schemas/integration/OmicsResponse.yaml) also has
  `resultsExhausted`; [ObjectsPage](schemas/integration/ObjectsPage.yaml) uses
  `pageRequest`, `content`, and `filteredCount`. SCIM uses a one-based `startIndex`
  and `count`. Check the actual parameters and response before changing paging
  descriptions. Some entity search endpoints, including those in
  [studyUser.yaml](studyUser.yaml) and [studyCurator.yaml](studyCurator.yaml),
  mention `resultsExhausted` although
  `ListResponse` does not declare it. Resolve this mismatch against the server
  or flag it; do not generalize it to other endpoints.
- Jobs return an integer `jobExecId` and use statuses in
  [Info](schemas/job/Info.yaml) and [Output](schemas/job/Output.yaml). Tasks use
  a string `id` and a different status enum in [TaskInfo](schemas/tasks/TaskInfo.yaml).
  Preserve their separate lifecycle descriptions. The publish-versions operation
  in [integrationCurator.yaml](integrationCurator.yaml) returns `TaskInfo`, not a
  job result. Deletion in [manageData.yaml](manageData.yaml) returns HTTP 202 for
  objects marked for deletion; do not describe acceptance as completed work.
- Keep link removal separate from object deletion. The DELETE operations under
  `/api/v1/as-curator/integration/link/...` and `/api/v1/as-curator/links` in
  [integrationCurator.yaml](integrationCurator.yaml) remove links.
  [manageData.yaml](manageData.yaml) documents object deletion across versions
  and linked objects, plus permissions that differ by object type. Its
  ownership-recovery operations have their own role and active-user constraints;
  do not replace these with a generic curator permission.
- URL imports and multipart uploads have distinct request models in `schemas/job/`.
  For example, [ImportMetadataRequest](schemas/job/ImportMetadataRequest.yaml)
  requires `metadataLink`, while
  [ImportMetadataFromMultipartRequest](schemas/job/ImportMetadataFromMultipartRequest.yaml)
  requires `studyId` and binary `file`. Multipart transformation fields are
  pipeline-populated and read-only in
  [TransformationParametersForMultipartRequest](schemas/job/TransformationParametersForMultipartRequest.yaml).
- Preserve each operation's authentication alternatives. Separate `Access-token`
  and `Genestack-API-Token` security entries describe alternatives, not a requirement
  to send both. The `integration/fulltext/search/studies` operations in both
  integration fragments currently declare only `Genestack-API-Token`; verify
  server support before adding another authentication option.
  SCIM uses `application/scim+json` for many responses and
  `application/json` for others. File downloads in [fileUser.yaml](fileUser.yaml)
  have binary responses, HEAD behavior, and partial content via `Range` and `206`.
  A neighboring JSON endpoint is not a suitable replacement pattern.

## Acceptance evidence

Apply the root hooks to changed files. Check `$ref` targets and fragments, repeated
aliases, operation ID uniqueness, and method/path collisions across affected
consumers. Syntax checks alone do not establish these properties. Preserve
specified 4xx responses and permission rejections; do not remove them merely to
make validation pass. Fix the source or build cause, or report the remaining
failure.

For structural or client-visible changes, use the
[generation and SDK checks](../../docs/agents/build-and-delivery.md#validation).
Inspect the affected Python and R signatures/models and serialized wire names,
plus the merged specification and affected standalone fragment. Generated tests
may be stubs, so passing them alone does not prove the API contract. Apply the
[root server-verification and live-target rules](../../AGENTS.md#shared-conventions)
when reporting acceptance evidence.
