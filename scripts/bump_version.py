#!/usr/bin/env python3
"""Bump the authoritative ChiseTweaks repository version deterministically."""
from __future__ import annotations

import argparse
import os
import tempfile
from pathlib import Path

from versioning_core import expected_bump, format_version, parse_version, read_properties, replace_property

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES_PATH = ROOT / "gradle.properties"


def compute_next_version(current: str, minecraft: str, release_type: str) -> str:
    current_core, current_minecraft = parse_version(current)
    if current_minecraft != minecraft:
        raise ValueError(
            f"mod_version Minecraft metadata mc{current_minecraft} does not match minecraft_version={minecraft}"
        )
    return format_version(expected_bump(current_core, release_type), minecraft)


def update_version_file(path: Path, release_type: str, *, dry_run: bool = False) -> tuple[str, str]:
    original = path.read_text(encoding="utf-8")
    properties = read_properties(path)
    current = properties["mod_version"]
    minecraft = properties["minecraft_version"]
    updated = compute_next_version(current, minecraft, release_type)
    new_text = replace_property(original, "mod_version", updated)

    if dry_run:
        return current, updated

    fd, temp_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    temp_path = Path(temp_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="") as handle:
            handle.write(new_text)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_path, path)
    finally:
        if temp_path.exists():
            temp_path.unlink()

    return current, updated


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("release_type", choices=("patch", "minor", "major"))
    parser.add_argument("--dry-run", action="store_true", help="Print the next version without writing files")
    args = parser.parse_args()

    try:
        current, updated = update_version_file(PROPERTIES_PATH, args.release_type, dry_run=args.dry_run)
    except (OSError, KeyError, ValueError) as error:
        print(f"VERSION BUMP: FAIL: {error}")
        return 1

    action = "DRY RUN" if args.dry_run else "UPDATED"
    print(f"VERSION BUMP: {action}")
    print(f"previous={current}")
    print(f"current={updated}")
    print(f"file={PROPERTIES_PATH.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
