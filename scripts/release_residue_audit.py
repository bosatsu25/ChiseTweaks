#!/usr/bin/env python3
"""Reject removed production classes and legacy visual assets from release JARs."""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIBS = ROOT / "build" / "libs"

FORBIDDEN_EXACT_ENTRIES = {
    "assets/chisetweaks/models/block/visual/diamond_ore.json",
    "assets/chisetweaks/models/block/visual/deepslate_diamond_ore.json",
    "assets/chisetweaks/textures/block/visual/diamond_ore_chise.png.mcmeta",
    "assets/chisetweaks/textures/block/visual/deepslate_diamond_ore_chise.png.mcmeta",
}
FORBIDDEN_ENTRY_TOKENS = (
    "BuilderEntityVisibilityPolicy",
    "ModVersionPolicy",
    "PreReleaseFeaturePolicy",
    "PreReleaseUiPolicy",
    "ChiseTextureVisibilitySetting",
    "PreReleaseMixinConfigPlugin",
)


def read_properties() -> dict[str, str]:
    result: dict[str, str] = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def audit_jar(path: Path) -> None:
    if not path.is_file():
        raise RuntimeError(f"release artifact is missing: {path}")
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        exact = sorted(names & FORBIDDEN_EXACT_ENTRIES)
        if exact:
            raise RuntimeError(f"legacy visual asset returned to {path.name}: {exact}")
        for token in FORBIDDEN_ENTRY_TOKENS:
            matches = sorted(name for name in names if token in name)
            if matches:
                raise RuntimeError(f"removed production entry returned to {path.name}: {matches[:5]}")


def main() -> int:
    try:
        properties = read_properties()
        version = properties["mod_version"]
        base = properties["archives_base_name"]
        audit_jar(LIBS / f"{base}-{version}.jar")
        audit_jar(LIBS / f"{base}-{version}-sources.jar")
        print("RELEASE RESIDUE AUDIT: PASS")
        print("legacy_architecture_residue=false")
        return 0
    except (OSError, KeyError, zipfile.BadZipFile, RuntimeError) as error:
        print(f"RELEASE RESIDUE AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
