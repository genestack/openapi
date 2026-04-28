import asyncio
import base64
import os
from typing import Optional
import httpx
from fastmcp import FastMCP, Client
from pydantic import BaseModel, Field

odm_url = os.environ.get("ODM_URL", "https://develop-oak.dev.gs.team").rstrip("/")
odm_token = os.environ.get("API_TOKEN", "tknRoot")

server_host = os.environ.get("SERVER_HOST", "0.0.0.0")
server_port = int(os.environ.get("SERVER_PORT", 8080))

mcp = FastMCP(name="ODM EDA MCP")

_headers = {"Genestack-API-Token": odm_token}

PAGE_LIMIT = 50
SEARCH_STUDIES_PATH = "/api/v1/as-user/integration/fulltext/search/studies"
SYSTEM_FILTER_GROUPS = {"GS_VALIDITY_FLAG", "GS_Access"}


class StudiesSearchParams(BaseModel):
    full_text: Optional[str] = Field(
        default=None,
        description=(
            "Free-text search across study metadata (STRICT match). "
            'Examples: "TCGA", "covid", "breast cancer".'
        ),
    )
    data_classes: Optional[list[str]] = Field(
        default=None,
        description=(
            "Filter by measurement data classes. Multiple values are AND-combined "
            "(study must have all). Closed list: "
            "Bulk transcriptomics, DNA methylation, Gene panel data, Gene variant (VCF), "
            "Proteomics, Single-cell transcriptomics, Flow Cytometry (FACS), Other. "
            'Example: ["Bulk transcriptomics", "Gene variant (VCF)"].'
        ),
    )
    metadata: Optional[dict[str, list[str]]] = Field(
        default=None,
        description=(
            "Filter by metadata fields. Keys are field names (spaces or underscores accepted), "
            "values are lists of option names. Multiple values for the same key are AND-combined. "
            "Common queryable fields: Therapeutic Area, Study Source, Disease, Organism, "
            "Tissue, Sample Type, Study Type, Sex, Cell Type, Platform Type, "
            "Tumor Histological Type, Sample Source, Genome Version, "
            "Library Preparation Protocol, Project ID, Experimental Platform, Data Species, "
            "Data Processing Method, Normalization Method, Study ID, Condition, "
            "Primary Tumor Anatomical Site Subdivision, Attached file attribute, Crop, Group, "
            "Tissue Type, Assay Type, Contact Institute, Organization name. "
            "Other metadata fields may also be queryable; the exact field/value names "
            "available for the current result set appear in `available_filters` of any "
            "prior response. "
            'Example: {"Therapeutic Area": ["Oncology"], "Disease": ["breast cancer"]}.'
        ),
    )


class FacetOption(BaseModel):
    name: str
    study_count: int


class FacetGroup(BaseModel):
    options: list[FacetOption]
    has_more: bool


class AvailableFilters(BaseModel):
    data_classes: FacetGroup = Field(
        description="Available GS_DATA_CLASS values to narrow on. Mirrors input `data_classes`."
    )
    metadata: dict[str, FacetGroup] = Field(
        description="Available metadata-field options. Keys mirror input `metadata` keys."
    )


class DataClassMatrixRow(BaseModel):
    data_classes: list[str]
    study_count: int
    sample_count: int


class DataClassTotal(BaseModel):
    study_count: int
    sample_count: int


class DataClassMatrix(BaseModel):
    columns: list[str] = Field(description="Data classes seen in returned studies, frequency desc.")
    rows: list[DataClassMatrixRow] = Field(
        description="Studies grouped by exact data-class signature; study_count desc."
    )
    totals_per_class: dict[str, DataClassTotal] = Field(
        description="Per-data-class totals across returned studies (partial when truncated)."
    )


class StudyEntry(BaseModel):
    accession: str
    name: str
    sample_count: int
    data_classes: list[str]
    summary: dict[str, list[str]]


class ExploreStudiesSummary(BaseModel):
    studies_found: int = Field(description="Total studies matching filters (exact).")
    studies_returned: int = Field(description="Studies included in this response (≤50).")
    truncated: bool = Field(description="True when studies_found > studies_returned.")
    samples_in_returned_studies: int = Field(
        description="Sum of sample counts across returned studies (partial when truncated)."
    )


class ExploreStudiesResult(BaseModel):
    summary: ExploreStudiesSummary
    data_class_matrix: DataClassMatrix
    studies: list[StudyEntry] = Field(description="Per-study details, sample_count desc, capped at 50.")
    available_filters: AvailableFilters


def _encode_filter_option_id(filter_id: str, name: str) -> str:
    return base64.b64encode(f"{filter_id}:{name}".encode("utf-8")).decode("ascii")


def _build_filters(params: StudiesSearchParams) -> list[dict]:
    filters: list[dict] = []
    if params.full_text:
        filters.append({"type": "FULL_TEXT", "match": params.full_text, "mode": "STRICT"})
    for name in params.data_classes or []:
        filters.append({
            "type": "SELECT",
            "filterOptionId": _encode_filter_option_id("GS_DATA_CLASS", name),
        })
    for raw_key, values in (params.metadata or {}).items():
        filter_id = f"METADATA_{raw_key.replace(' ', '_')}"
        for value in values:
            filters.append({
                "type": "SELECT",
                "filterOptionId": _encode_filter_option_id(filter_id, value),
            })
    return filters


def _build_studies(content: list[dict]) -> list[StudyEntry]:
    studies = [
        StudyEntry(
            accession=s["accession"],
            name=s["name"],
            sample_count=s.get("size", 0),
            data_classes=[dc["title"] for dc in s.get("dataClasses", [])],
            summary={entry["key"]: entry.get("values", []) for entry in s.get("summary", [])},
        )
        for s in content
    ]
    studies.sort(key=lambda x: x.sample_count, reverse=True)
    return studies


def _build_matrix(studies: list[StudyEntry]) -> DataClassMatrix:
    class_studies: dict[str, int] = {}
    class_samples: dict[str, int] = {}
    for s in studies:
        for c in s.data_classes:
            class_studies[c] = class_studies.get(c, 0) + 1
            class_samples[c] = class_samples.get(c, 0) + s.sample_count

    columns = sorted(class_studies, key=lambda c: class_studies[c], reverse=True)
    column_index = {c: i for i, c in enumerate(columns)}
    totals_per_class = {
        c: DataClassTotal(study_count=class_studies[c], sample_count=class_samples[c])
        for c in columns
    }

    groups: dict[frozenset, list[StudyEntry]] = {}
    for s in studies:
        groups.setdefault(frozenset(s.data_classes), []).append(s)

    rows = [
        DataClassMatrixRow(
            data_classes=sorted(key, key=lambda c: column_index.get(c, len(columns))),
            study_count=len(group),
            sample_count=sum(s.sample_count for s in group),
        )
        for key, group in groups.items()
    ]
    rows.sort(key=lambda r: r.study_count, reverse=True)
    return DataClassMatrix(columns=columns, rows=rows, totals_per_class=totals_per_class)


def _build_available_filters(filter_option_groups: list[dict]) -> AvailableFilters:
    data_classes_group = FacetGroup(options=[], has_more=False)
    metadata_groups: dict[str, FacetGroup] = {}

    for group in filter_option_groups:
        filter_id = group.get("filterId", "")
        if filter_id in SYSTEM_FILTER_GROUPS:
            continue
        options = [
            FacetOption(name=o["name"], study_count=o["count"])
            for o in group.get("options", [])
            if o.get("count") is not None
        ]
        facet = FacetGroup(options=options, has_more=bool(group.get("hasMoreOptions", False)))
        if filter_id == "GS_DATA_CLASS":
            data_classes_group = facet
        elif filter_id.startswith("METADATA_"):
            label = filter_id[len("METADATA_"):].replace("_", " ")
            metadata_groups[label] = facet

    return AvailableFilters(data_classes=data_classes_group, metadata=metadata_groups)


@mcp.tool
def explore_studies(search_params: StudiesSearchParams) -> ExploreStudiesResult:
    """Iteratively define a study cohort for a planned bioinformatics experiment.

    Each call returns:
      - summary stats (studies & samples found),
      - a data-class availability matrix grouping studies by their exact set of
        measurement types,
      - per-study details (capped at 50; check `summary.truncated`),
      - `available_filters` showing which filter options will narrow further.

    Strategy:
      - If you don't know where to start, call with no filters first. The response
        surfaces the most populated facets — pick one to narrow on.
      - Then add filters and call again to refine. Backtrack by removing filters.
      - When `summary.truncated` is true (studies_found > 50), narrow further before
        trusting the matrix; per-data-class sample counts are partial in that case.

    Inputs:
      - full_text: free-text search (STRICT match). E.g. "TCGA", "covid".
      - data_classes: closed list of measurement types to require, e.g.
        ["Bulk transcriptomics", "Gene variant (VCF)"]. AND-combined.
      - metadata: dict of metadata field → list of allowed values, e.g.
        {"Therapeutic Area": ["Oncology"], "Disease": ["breast cancer"]}.
        Values for the same key are AND-combined.

    The exact field/value names you can use appear in `available_filters` on every
    response — copy them directly into the next call.
    """
    body = {
        "filters": _build_filters(search_params),
        "page": {"offset": 0, "limit": PAGE_LIMIT},
    }
    response = httpx.post(
        f"{odm_url}{SEARCH_STUDIES_PATH}",
        headers={**_headers, "Content-Type": "application/json", "accept": "application/json"},
        json=body,
    )
    response.raise_for_status()
    data = response.json()

    page = data.get("page", {})
    filtered_count = page.get("filteredCount", 0)
    studies = _build_studies(page.get("content", []))
    matrix = _build_matrix(studies)
    available = _build_available_filters(data.get("filterOptionGroups", []))

    return ExploreStudiesResult(
        summary=ExploreStudiesSummary(
            studies_found=filtered_count,
            studies_returned=len(studies),
            truncated=filtered_count > len(studies),
            samples_in_returned_studies=sum(s.sample_count for s in studies),
        ),
        data_class_matrix=matrix,
        studies=studies,
        available_filters=available,
    )


# class SamplesSearchParams(BaseModel):
#     pass
#
#
# @mcp.tool
# def explore_samples(search_params: SamplesSearchParams) -> dict:
#     """Find what samples is available
#     """
#     pass
#
#
# @mcp.tool
# def preview_samples_cohort(search_params: SamplesSearchParams) -> dict:
#     """When ready to explore in details, reuse query to
#     Fetches up to 2000 samples from study with metadata, returns column names, values distributions, across all intersecting columns.
#     Doesn't include all the data, but marks if there were more than 2000 samples.
#     """


# @mcp.tool
# def get_study_by_id(
#     accession_id: str
# ) -> dict:
#     """
#     Retrieve a single study by its Genestack accession ID.

#     accession_id: Genestack accession ID (e.g. GSF1678476)
#     """
#     response = httpx.get(
#         f"{odm_url}/api/v1/as-user/studies/{accession_id}",
#         headers=_headers,
#         params={"returnedMetadataFields": "minimal_data"},
#     )
#     response.raise_for_status()
#     return response.json()

# @mcp.tool
# def inspect_omics_endpoint(
#     params: dict,
# ) -> dict:
#     """
#     params
#     """
#     params['page_limit'] = 5
#     response = httpx.get(
#         f"{odm_url}/api/v1/as-user/omics/samples",
#         headers=_headers,
#         params=params,
#     )
#     response.raise_for_status()
#     return response.json()


async def test_func():
    cases: list[tuple[str, dict]] = [
        ("covid", {"full_text": "covid"}),
        ("tcga + oncology", {"full_text": "TCGA", "metadata": {"Therapeutic Area": ["Oncology"]}}),
        ("empty (discovery)", {}),
    ]
    async with Client(mcp) as client:
        for label, search_params in cases:
            print(f"\n=== explore_studies: {label} ===")
            result = await client.call_tool("explore_studies", {"search_params": search_params})
            d = result.data
            print(
                f"summary: found={d.summary.studies_found} "
                f"returned={d.summary.studies_returned} "
                f"truncated={d.summary.truncated} "
                f"sample_sum={d.summary.samples_in_returned_studies}"
            )
            print(f"matrix columns: {d.data_class_matrix.columns}")
            print("matrix rows:")
            for r in d.data_class_matrix.rows:
                print(f"  studies={r.study_count} samples={r.sample_count} dc={r.data_classes}")
            print("totals_per_class:")
            for c, t in d.data_class_matrix.totals_per_class.items():
                print(f"  {c}: studies={t.study_count} samples={t.sample_count}")
            print(
                "available_filters.data_classes: "
                f"{[(o.name, o.study_count) for o in d.available_filters.data_classes.options]} "
                f"has_more={d.available_filters.data_classes.has_more}"
            )
            print("available_filters.metadata:")
            for k, g in d.available_filters.metadata.items():
                print(f"  {k} (has_more={g.has_more}): {[(o.name, o.study_count) for o in g.options]}")
            if d.studies:
                top = d.studies[0]
                print(f"top study: {top.accession} {top.name!r} samples={top.sample_count}")


if __name__ == "__main__":
    # asyncio.run(test_func())
    mcp.run(transport="streamable-http", host=server_host, port=server_port)
