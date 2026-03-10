import os
from pathlib import Path

import httpx
import yaml
from fastmcp import FastMCP
from pathlib import Path

odm_url = os.environ.get("ODM_URL", 'https://develop-oak.dev.gs.team') # pre-defined value for local testing
odm_token = os.environ.get("ODM_TOKEN", "tknRoot")
server_host = os.environ.get("SERVER_HOST", "0.0.0.0")
server_port = os.environ.get("SERVER_PORT", 9080)

# Load OpenAPI spec from the same directory as this file
spec_path = Path(__file__).with_name("odmApi.yaml")
with spec_path.open("r", encoding="utf-8") as fh:
    openapi_spec = yaml.safe_load(fh)

user_tags: list[str] = [tag["name"] for tag in openapi_spec["tags"] if "as User" in tag["name"]]
curator_tags: list[str] = [tag["name"] for tag in openapi_spec["tags"] if "as Curator" in tag["name"]]

# Map $ref string -> schema dict, e.g. "#/components/schemas/Study" -> {...}
schemas_by_ref: dict[str, dict] = {
    f"#/components/schemas/{name}": schema
    for name, schema in openapi_spec["components"]["schemas"].items()
}

# Map tag -> list of {endpoint, method, summary, operationId}
methods_by_tag: dict[str, list[dict]] = {}
for _path, _path_item in openapi_spec["paths"].items():
    for _method, _operation in _path_item.items():
        if not isinstance(_operation, dict):
            continue
        for _tag in _operation.get("tags", []):
            methods_by_tag.setdefault(_tag, []).append({
                "endpoint": _path,
                "method": _method,
                "summary": _operation.get("summary", ""),
                "operationId": _operation.get("operationId", ""),
            })

mcp = FastMCP(
    name="ODM API docs MCP Server",
    instructions="""
        This server provides documentation on using the ODM API for data querying, retrieval, import (ingestion), and curation.
        Its main purpose is to provide the LLM with correct schemas of endpoints.
        Call get_odm_api_overview() to get more details about the API.
    """
)

type Tag = str

@mcp.tool(description="Fetch url of the ODM API server")
def get_base_url() -> str:
    return odm_url
    
@mcp.tool(description="Get an overview of functions available in ODM")
def get_odm_api_overview() -> str:
    return """
        - You can explore and retrieve data from the ODM using the API endpoints. These endpoints are marked with suffix "as User". Get a list of all tools by calling "list_query_retrieval_endpoints" tool.
        - You can also create studies and curate data. The main functions available are Create a new study and Curate Data. These endpoints are marked with suffix "as Curator". Get a list of all tools by calling "list_import_curation_endpoints" tool.
    """

@mcp.tool(description="List all available querying and retrieval endpoint groups (tags)")
def list_query_retrieval_endpoints() -> list[Tag]:
    return user_tags

@mcp.tool(description="List all available import and curation endpoint groups (tags)")
def list_import_curation_endpoints() -> list[Tag]:
    return curator_tags

@mcp.tool(description='List all paths with descriptions that belong to a specific group (tag) (e.g. "Study SPoT as User")')
def list_methods_in_group(group: Tag) -> list[dict]:
    return methods_by_tag.get(group, [])

@mcp.tool(description='Get operations, parameters, responses and schemas of an endpoint (e.g. "/api/v1/as-curator/studies/{id}")')
def get_path_item(endpoint: str) -> dict:
    return openapi_spec["paths"].get(endpoint, {})

@mcp.tool(description='Get data schema definition by $ref (e.g. "#/components/schemas/Study")')
def get_schema_by_ref(ref: str) -> dict:
    return schemas_by_ref.get(ref, {})


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host=server_host, port=server_port)
