#!/usr/bin/env python3
"""Validate ChiseTweaks SemVer and optional release-bump intent."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_RE = re.compile(
    r"^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)\+mc(?P<minecraft>\d+\.\d+\.\d+)$"
)


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def parse_version(value: str) -> tuple[tuple[int, int, int], str]:
    match = VERSION_RE.fullmatch(value)
    if match is None:
        raise ValueError(
            "version must be MAJOR.MINOR.PATCH+mc<Minecraft>, for example 0.8.0+mc26.1.2"
        )
    core = (
        int(match.group("major")),
        int(match.group("minor")),
        int(match.group("patch")),
    )
    return core, match.group("minecraft")


def expected_bump(previous: tuple[int, int, int], release_type: str) -> tuple[int, int, int]:
    major, minor, patch = previous
    if release_type == "patch":
        return major, minor, patch + 1
    if release_type == "minor":
        return major, minor + 1, 0
    if release_type == "major":
        return major + 1, 0, 0
    raise ValueError(f"unsupported release type: {release_type}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous", help="Previous released version, without a leading v")
    parser.add_argument("--release-type", choices=("patch", "minor", "major"))
    args = parser.parse_args()

    try:
        properties = read_properties(ROOT / "gradle.properties")
        current = properties["mod_version"]
        minecraft = properties["minecraft_version"]
        current_core, current_minecraft = parse_version(current)
        if current_minecraft != minecraft:
            raise ValueError(
                f"mod_version Minecraft metadata mc{current_minecraft} does not match minecraft_version={minecraft}"
            )

        if bool(args.previous) != bool(args.release_type):
            raise ValueError("--previous and --release-type must be supplied together")

        if args.previous:
            previous_core, _ = parse_version(args.previous)
            expected = expected_bump(previous_core, args.release_type)
            if current_core != expected:
                expected_text = ".".join(str(value) for value in expected)
                current_text = ".".join(str(value) for value in current_core)
                raise ValueError(
                    f"{args.release_type} release requires SemVer core {expected_text}, found {current_text}"
                )

        print("VERSION POLICY: PASS")
        print(f"version={current}")
        print(f"semver_core={'.'.join(str(value) for value in current_core)}")
        print(f"minecraft={minecraft}")
        return 0
    except (OSError, KeyError, ValueError) as error:
        print(f"VERSION POLICY: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
