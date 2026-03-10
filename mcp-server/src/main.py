import os
from pathlib import Path

import httpx
import yaml
from fastmcp import FastMCP
from fastmcp.resources import HttpResource

odm_url = os.environ.get("ODM_URL")
# odm_url = os.environ.get("ODM_URL", 'https://develop-oak.dev.gs.team'). # for local testing
odm_token = os.environ.get("ODM_TOKEN", "tknRoot")
server_host = os.environ.get("SERVER_HOST", "0.0.0.0")
server_port = os.environ.get("SERVER_PORT", 8080)

# Create an HTTP client for ODM API
client = httpx.AsyncClient(
    base_url=odm_url,
    headers={"Genestack-API-Token": f"{odm_token}"}
)

# Load OpenAPI spec from the same directory as this file
spec_path = Path(__file__).with_name("odmApi.yaml")
with spec_path.open("r", encoding="utf-8") as fh:
    openapi_spec = yaml.safe_load(fh)

# Create the MCP server
mcp = FastMCP.from_openapi(
    name="Openapi MCP Server",
    openapi_spec=openapi_spec,
    client=client,
)

mcp.add_resource(HttpResource(
    uri="resource://documentation-yaml",
    url=odm_url + "/swagger/yaml/odm.yaml",
    name="ODM API Documentation",
    description="ODM API documentation in yaml ",
    mime_type="application/yaml"
))


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host=server_host, port=server_port)
