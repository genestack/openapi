import os
from pathlib import Path

import httpx
import yaml
from fastmcp import FastMCP
from pathlib import Path

DOCS_DIR = Path("docs").resolve()

odm_url = os.environ.get("ODM_URL", 'https://develop-oak.dev.gs.team') # for local testing
odm_token = os.environ.get("ODM_TOKEN", "tknRoot")
server_host = os.environ.get("SERVER_HOST", "0.0.0.0")
server_port = os.environ.get("SERVER_PORT", 9080)

# Load OpenAPI spec from the same directory as this file
spec_path = DOCS_DIR / "odmApi.yaml"
with spec_path.open("r", encoding="utf-8") as fh:
    openapi_spec = yaml.safe_load(fh)

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
    return "This is an overview"

@mcp.tool(description="List all available querying and retrieval endpoint groups (tags)")
def list_query_retrieval_endpoints() -> list[Tag]:
    return ['a', 'b']

@mcp.tool(description="List all available import and curation endpoint groups (tags)")
def list_import_curation_endpoints() -> list[Tag]:
    return ['c', 'd']

@mcp.tool(description="List all paths with descriptions that belong to a specific group (tag)")
def list_methods_in_group(group: Tag) -> list[dict]:
    return [{'endpoint': '/test/test', 'method': 'get', 'summary': 'This is test'}]

@mcp.tool(description='Get operations, parameters, responces and schemas of an enpoint')
def get_path_item(endpoint: str) -> dict:
    return {'test': 'test'}

@mcp.tool(description='Get data schema definition by $ref')
def get_schema_by_ref(ref: str) -> dict:
    return {'schema': 'test'}


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host=server_host, port=server_port)
