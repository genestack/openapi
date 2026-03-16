import os
from contextvars import ContextVar
from pathlib import Path
from typing import Any

import httpx
import yaml
from fastmcp import FastMCP
from fastmcp.server.providers.openapi import MCPType
from fastmcp.utilities.openapi.models import HTTPRoute
from fastmcp.server.middleware import Middleware, MiddlewareContext, CallNext
from fastmcp.server.dependencies import get_http_headers

# Per-request storage for the API token.
# ContextVar is asyncio-safe: each concurrent request gets its own isolated value.
current_token: ContextVar[str] = ContextVar("current_token", default="")

odm_url = os.environ.get("ODM_URL", "https://odm.demo.genestack.com/")
server_host = os.environ.get("SERVER_HOST", "0.0.0.0")
server_port = int(os.environ.get("SERVER_PORT", 8080))

spec_path = Path(__file__).with_name("odmApi.yaml")
with spec_path.open("r", encoding="utf-8") as fh:
    openapi_spec = yaml.safe_load(fh)


class DynamicTokenAuth(httpx.Auth):
    """Injects the per-request token into every outgoing ODM API call.

    Reads from current_token ContextVar, so each concurrent MCP request
    carries its own token without shared state.
    """
    def auth_flow(self, request: httpx.Request):
        token = current_token.get()
        if token:
            request.headers["Genestack-API-Token"] = token
        yield request


class TokenExtractMiddleware(Middleware):
    """Extracts the API token from the incoming HTTP request and stores it
    in current_token for the duration of the MCP message handling.
    """
    async def on_message(self, context: MiddlewareContext[Any], call_next: CallNext) -> Any:
        headers = get_http_headers(include={"x-genestack-api-token", "authorization"})
        token = (
            headers.get("x-genestack-api-token")
            or headers.get("authorization", "").removeprefix("Bearer ")
        )
        token_var = current_token.set(token)
        try:
            return await call_next(context)
        finally:
            # Reset to previous value to avoid leaking across reused tasks.
            current_token.reset(token_var)


INCLUDED_ENDPOINTS: set[str] = {
    # Core Resources — list + get by ID
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
    # Link Discovery
    "/api/v1/as-user/links",
    "/api/v1/as-user/links/get-batch",
    # Schema Discovery
    "/api/v1/as-user/data-types",
    "/api/v1/as-user/data-types/links",
    # Search
    "/api/v1/as-user/integration/fulltext/search/studies",
    # Omics Data & Analytics
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


def endpoint_filter(route: HTTPRoute, mcp_type: MCPType) -> MCPType | None:
    if route.path in INCLUDED_ENDPOINTS:
        return MCPType.TOOL
    return MCPType.EXCLUDE


# Single shared client — connection pooling is safe because auth is per-request via DynamicTokenAuth.
client = httpx.AsyncClient(base_url=odm_url, auth=DynamicTokenAuth())

mcp = FastMCP.from_openapi(
    name="Openapi MCP Server",
    openapi_spec=openapi_spec,
    client=client,
    middleware=[TokenExtractMiddleware()],
    route_map_fn=endpoint_filter,
)

@mcp.tool(description="Returns url of ODM API server")
def get_base_url() -> str:
    return "https://odm.demo.genestack.com/"
    # return odm_url


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host=server_host, port=server_port)