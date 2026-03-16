# Plan: Simplify `/as-user/` Endpoints for MCP Server

## Context
The odmApi.yaml has ~97 `/as-user/` endpoints. For LLM usage via MCP, fewer tools improves comprehension and tool selection accuracy. We're removing redundant endpoints without losing any capabilities. Auth level: read-only (user tier only). Link strategy: generic `/links` only.

## Endpoints to KEEP (~37 endpoints)

### Core Resources — list + get by ID (17 endpoints)
| # | Endpoint | Purpose |
|---|----------|---------|
| 1 | `GET /as-user/cells/{id}` | Get cell by ID |
| 2 | `GET /as-user/expressions` | List/search expressions |
| 3 | `GET /as-user/expressions/{id}` | Get expression by ID |
| 4 | `GET /as-user/files` | List/search files |
| 5 | `GET /as-user/files/{id}` | Get file metadata by ID |
| 6 | `GET /as-user/files/{id}/download` | Download file content |
| 7 | `GET /as-user/flow-cytometries` | List/search flow cytometries |
| 8 | `GET /as-user/flow-cytometries/{id}` | Get flow cytometry by ID |
| 9 | `GET /as-user/libraries` | List/search libraries |
| 10 | `GET /as-user/libraries/{id}` | Get library by ID |
| 11 | `GET /as-user/preparations` | List/search preparations |
| 12 | `GET /as-user/preparations/{id}` | Get preparation by ID |
| 13 | `GET /as-user/samples` | List/search samples |
| 14 | `GET /as-user/samples/{id}` | Get sample by ID |
| 15 | `GET /as-user/studies` | List/search studies |
| 16 | `GET /as-user/studies/{id}` | Get study by ID |
| 17 | `GET /as-user/variants` | List/search variants |
| 18 | `GET /as-user/variants/{id}` | Get variant by ID |

### Link Discovery (2 endpoints)
| # | Endpoint | Purpose |
|---|----------|---------|
| 19 | `GET /as-user/links` | Find links by source/target ID and type |
| 20 | `POST /as-user/links/get-batch` | Batch link discovery by IDs |

### Schema Discovery (2 endpoints)
| # | Endpoint | Purpose |
|---|----------|---------|
| 21 | `GET /as-user/data-types` | List available data types |
| 22 | `GET /as-user/data-types/links` | List valid link types between data types |

### Search (1 endpoint)
| # | Endpoint | Purpose |
|---|----------|---------|
| 23 | `GET /as-user/integration/fulltext/search/studies` | Full-text search across studies |

### Omics Data & Analytics (14 endpoints)
| # | Endpoint | Purpose |
|---|----------|---------|
| 24 | `GET /as-user/omics/cells` | Query cells with linked data filters |
| 25 | `GET /as-user/omics/cells/expression/data` | Cell-level expression data |
| 26 | `GET /as-user/omics/expression/data` | Expression measurement data |
| 27 | `GET /as-user/omics/expression/group` | Expression group data |
| 28 | `GET /as-user/omics/expression/streamed-data` | Streamed expression data |
| 29 | `GET /as-user/omics/flow-cytometry/data` | Flow cytometry measurement data |
| 30 | `GET /as-user/omics/flow-cytometry/group` | Flow cytometry group data |
| 31 | `GET /as-user/omics/samples` | Sample omics data |
| 32 | `GET /as-user/omics/variant/data` | Variant measurement data |
| 33 | `GET /as-user/omics/variant/group` | Variant group data |
| 34 | `GET /as-user/omics/variant/streamed-data` | Streamed variant data |
| 35 | `POST /as-user/omics/cells/analytics/cell-ratio` | Cell ratio statistics |
| 36 | `POST /as-user/omics/cells/analytics/differential-expression` | Differential gene expression |
| 37 | `POST /as-user/omics/cells/analytics/gene-summary` | Gene expression summary |

---

## Endpoints to OMIT (~60 endpoints)

### Version Endpoints — 16 endpoints
`/{resource}/{id}/versions` and `/{resource}/{id}/versions/{version}` for: expressions, flow-cytometries, variants, samples, studies, libraries, preparations, cells.
**Reason**: LLM always wants current version, returned by `GET /{resource}/{id}`.

### By-Group Shortcuts — 4 endpoints
- `cells/by/group/{id}`, `libraries/by/group/{id}`, `preparations/by/group/{id}`, `expressions/by/group/{id}` (if exists)
**Reason**: Main list endpoint with filters achieves the same.

### Group/Run Navigation — 9 endpoints
`/{resource}/group`, `/{resource}/group/{id}`, `/{resource}/group/by/run/{id}`, `/{resource}/runs/by/group/{id}` for expressions, flow-cytometries, variants.
**Reason**: Internal organizational concept; adds LLM cognitive overhead without unique capability.

### ALL Specific Link Endpoints — ~30 endpoints
Every endpoint under `/as-user/integration/link/` including:
- `expression/by/library/{id}`, `expression/by/preparation/{id}`, `expression/by/sample/{id}`
- `expression/group/by/study/{id}`, `expression/run-to-*`
- `flow-cytometry/by/sample/{id}`, `flow-cytometry/group/by/study/{id}`, `flow-cytometry/run-to-*`
- `variant/by/sample/{id}`, `variant/group/by/study/{id}`, `variant/run-to-*`
- `library/by/sample/{id}`, `library/group/by/study/{id}`, `library/libraries-to-samples/*`
- `preparation/by/sample/{id}`, `preparation/group/by/study/{id}`, `preparation/preparations-to-samples/*`
- `samples/by/libraries`, `samples/by/preparations`, `samples/by/study/{id}`
- `studies/by/libraries`, `studies/by/preparations`, `studies/by/samples`, `studies/by/files`
- `study/by/sample/{id}`, `study/by/file/{id}`
- `files/by/study/{id}`
**Reason**: All replaced by generic `GET /links` + `POST /links/get-batch`. LLM uses 2-step: discover link IDs → fetch resources.

---

## Result
**~62% reduction**: from ~97 endpoints down to ~37, with zero capability loss.

## Implementation

### Use `route_map_fn` with an explicit include-list in `mcp-server/src/main.py`

Instead of fragile exclude-regex patterns, use `route_map_fn` — a callback that receives each `HTTPRoute` and returns the desired `MCPType`. This lets us define an explicit set of included endpoint paths, which is easier to read, maintain, and verify.

The `route_map_fn` callback signature (from `fastmcp.server.providers.openapi.routing`):
```python
Callable[[HTTPRoute, MCPType], MCPType | None]
```

`HTTPRoute.path` contains the OpenAPI path template (e.g., `/api/v1/as-user/cells/{id}`).

#### 1. Define the include-list

```python
from fastmcp.utilities.openapi.models import HTTPRoute

INCLUDED_ENDPOINTS: set[str] = {
    # Core Resources — list + get by ID (18 endpoints)
    "/api/v1/as-user/cells/{id}",
    "/api/v1/as-user/expressions",
    "/api/v1/as-user/expressions/{id}",
    "/api/v1/as-user/files",
    "/api/v1/as-user/files/{id}",
    "/api/v1/as-user/files/{id}/download",
    "/api/v1/as-user/flow-cytometries",
    "/api/v1/as-user/flow-cytometries/{id}",
    "/api/v1/as-user/libraries",
    "/api/v1/as-user/libraries/{id}",
    "/api/v1/as-user/preparations",
    "/api/v1/as-user/preparations/{id}",
    "/api/v1/as-user/samples",
    "/api/v1/as-user/samples/{id}",
    "/api/v1/as-user/studies",
    "/api/v1/as-user/studies/{id}",
    "/api/v1/as-user/variants",
    "/api/v1/as-user/variants/{id}",
    # Link Discovery (2 endpoints)
    "/api/v1/as-user/links",
    "/api/v1/as-user/links/get-batch",
    # Schema Discovery (2 endpoints)
    "/api/v1/as-user/data-types",
    "/api/v1/as-user/data-types/links",
    # Search (1 endpoint)
    "/api/v1/as-user/integration/fulltext/search/studies",
    # Omics Data & Analytics (14 endpoints)
    "/api/v1/as-user/omics/cells",
    "/api/v1/as-user/omics/cells/expression/data",
    "/api/v1/as-user/omics/expression/data",
    "/api/v1/as-user/omics/expression/group",
    "/api/v1/as-user/omics/expression/streamed-data",
    "/api/v1/as-user/omics/flow-cytometry/data",
    "/api/v1/as-user/omics/flow-cytometry/group",
    "/api/v1/as-user/omics/samples",
    "/api/v1/as-user/omics/variant/data",
    "/api/v1/as-user/omics/variant/group",
    "/api/v1/as-user/omics/variant/streamed-data",
    "/api/v1/as-user/omics/cells/analytics/cell-ratio",
    "/api/v1/as-user/omics/cells/analytics/differential-expression",
    "/api/v1/as-user/omics/cells/analytics/gene-summary",
}
```

#### 2. Define the filter function

```python
def endpoint_filter(route: HTTPRoute, mcp_type: MCPType) -> MCPType | None:
    if route.path in INCLUDED_ENDPOINTS:
        return MCPType.TOOL
    return MCPType.EXCLUDE
```

#### 3. Update `FastMCP.from_openapi()` call

Replace the current `route_maps` parameter with `route_map_fn`:

```python
mcp = FastMCP.from_openapi(
    name="Openapi MCP Server",
    openapi_spec=openapi_spec,
    client=client,
    middleware=[TokenExtractMiddleware()],
    route_map_fn=endpoint_filter,
)
```

#### Why include-list over exclude-regex

- **Explicit**: you see exactly which 37 endpoints are exposed — no reasoning about regex overlap
- **Safe by default**: new endpoints added to the spec are excluded until explicitly added to the set
- **Easy to modify**: add or remove a single path string, no regex debugging
- **Matches the plan**: the set mirrors the "Endpoints to KEEP" table above, 1:1

## Verification
1. Start the MCP server and list available tools — should see ~37 tools instead of ~97
2. Verify these are included: `findExpressionDataAsUser`, `getExpressionDataByIdAsUser`, `getLinksByParamsAsUser`, `getLinksByIdsAsUser`, `searchStudiesAsUser`
3. Verify these are excluded: `getExpressionByLibraryAsUser` (specific link), `getExpressionGroupByRunIdAsUser` (group/run), any `*versions*` operationId
4. For each omitted capability, verify achievable via kept endpoints:
   - Version lookup → `GET /{resource}/{id}` returns active version
   - Link traversal → `GET /links?firstId=X&firstType=Y&secondType=Z`
   - Group/run queries → `GET /{resource}?filter=...`
