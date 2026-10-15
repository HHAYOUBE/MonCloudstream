#!/usr/bin/env python3
"""Static checks for the AnimeSama and Senpai-Stream CloudStream providers."""
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parent.parent
PROVIDERS = {
    "AnimeSama": ROOT / "AnimeSama/src/main/kotlin/com/hhayoube/animesama/AnimeSamaProvider.kt",
    "SenpaiStream": ROOT / "SenpaiStream/src/main/kotlin/com/hhayoube/senpaistream/SenpaiStreamProvider.kt",
}


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    loaded = {}
    for name, path in PROVIDERS.items():
        if not path.is_file():
            fail(f"Missing provider source: {path.relative_to(ROOT)}")
        source = path.read_text(encoding="utf-8")
        loaded[name] = source
        lowered = source.lower()
        if "youtube.com" in lowered or "youtu.be" in lowered:
            fail(f"{name} still contains a YouTube host reference.")
        if "fun isofficialstreaminghost" not in lowered:
            fail(f"{name} is missing its official-host allowlist.")
        if "newextractorlink" not in lowered or "loadlinks(" not in lowered:
            fail(f"{name} is missing its CloudStream link-loading implementation.")
        if 'uri.scheme != "https" || uri.host.isnullorblank()' not in lowered:
            fail(f"{name} must reject non-HTTPS direct video links.")
        if "comingsoon = episodesources.isempty()" not in lowered:
            fail(f"{name} must mark entries without sources as coming soon.")

    senpai = loaded["SenpaiStream"]
    for token in ("TvType.Movie", "newMovieSearchResponse", "newMovieLoadResponse", "format"):
        if token not in senpai:
            fail(f"Senpai-Stream movie support is incomplete: missing {token}.")

    repo_file = ROOT / "repo.json"
    try:
        manifest = json.loads(repo_file.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"Cannot parse repo.json: {exc}")
    expected = "https://raw.githubusercontent.com/HHAYOUBE/MonCloudstream/builds/plugins.json"
    if expected not in manifest.get("pluginLists", []):
        fail("repo.json does not point to the verified builds/plugins.json location.")

    print("Extension source checks OK: providers, no YouTube, Senpai movie metadata, repository manifest.")


if __name__ == "__main__":
    main()
