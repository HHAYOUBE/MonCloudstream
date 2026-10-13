#!/usr/bin/env python3
"""Validate the editable source catalog without making network requests.

Network checks are intentionally avoided: public sites may block CI, rate-limit
requests, or change availability temporarily. This checks catalog structure and
safe, stable URL formatting only.
"""
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

CATALOG = Path("Sources/sources.json")
REQUIRED = ("id", "name", "url", "type", "description")
ID_PATTERN = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    try:
        data = json.loads(CATALOG.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(f"Missing catalog: {CATALOG}")
    except json.JSONDecodeError as exc:
        fail(f"Invalid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}")

    if not isinstance(data, dict):
        fail("Catalog root must be a JSON object.")
    if data.get("schemaVersion") != 1:
        fail("schemaVersion must be 1.")
    sources = data.get("sources")
    if not isinstance(sources, list):
        fail("'sources' must be an array.")

    seen_ids = set()
    for index, source in enumerate(sources, start=1):
        label = f"sources[{index - 1}]"
        if not isinstance(source, dict):
            fail(f"{label} must be an object.")
        missing = [key for key in REQUIRED if not isinstance(source.get(key), str) or not source[key].strip()]
        if missing:
            fail(f"{label} is missing non-empty string field(s): {', '.join(missing)}")

        source_id = source["id"]
        if not ID_PATTERN.fullmatch(source_id):
            fail(f"{label}.id must use lowercase letters, numbers, and single hyphens.")
        if source_id in seen_ids:
            fail(f"Duplicate source id: {source_id}")
        seen_ids.add(source_id)

        parsed = urlparse(source["url"])
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
            fail(f"{label}.url must be a valid HTTPS URL without embedded credentials.")
        if any(char.isspace() for char in source["url"]):
            fail(f"{label}.url must not contain whitespace.")

    print(f"Source catalog OK: {len(sources)} entries validated.")


if __name__ == "__main__":
    main()
