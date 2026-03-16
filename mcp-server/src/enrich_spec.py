from pathlib import Path

import yaml


def _resolve_node(node: object, base_dir: Path) -> object:
    if isinstance(node, dict):
        if "$ref" in node and len(node) == 1:
            ref: str = node["$ref"]
            if not ref.startswith("#"):
                schema_file = (base_dir / ref).resolve()
                with schema_file.open("r", encoding="utf-8") as fh:
                    loaded = yaml.safe_load(fh)
                return _resolve_node(loaded, schema_file.parent)
        return {k: _resolve_node(v, base_dir) for k, v in node.items()}
    if isinstance(node, list):
        return [_resolve_node(item, base_dir) for item in node]
    return node


if __name__ == "__main__":
    spec_path = Path(__file__).with_name("odmApi.yaml")
    with spec_path.open("r", encoding="utf-8") as fh:
        spec: dict = yaml.safe_load(fh)

    schemas: dict = spec.get("components", {}).get("schemas", {})
    for name, value in list(schemas.items()):
        schemas[name] = _resolve_node(value, spec_path.parent)

    with spec_path.open("w", encoding="utf-8") as fh:
        yaml.dump(spec, fh, allow_unicode=True, default_flow_style=False, sort_keys=False)
    print(f"Enriched {spec_path}")
