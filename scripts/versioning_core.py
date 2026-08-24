#!/usr/bin/env python3
"""Shared deterministic SemVer helpers for ChiseTweaks repository tooling."""
from __future__ import annotations

import re
from pathlib import Path

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
            "version must be MAJOR.MINOR.PATCH+mc<Minecraft>, for example 0.10.0+mc26.1.2"
        )
    return (
        int(match.group("major")),
        int(match.group("minor")),
        int(match.group("patch")),
    ), match.group("minecraft")


def format_version(core: tuple[int, int, int], minecraft: str) -> str:
    return f"{core[0]}.{core[1]}.{core[2]}+mc{minecraft}"


def expected_bump(previous: tuple[int, int, int], release_type: str) -> tuple[int, int, int]:
    major, minor, patch = previous
    if release_type == "patch":
        return major, minor, patch + 1
    if release_type == "minor":
        return major, minor + 1, 0
    if release_type == "major":
        return major + 1, 0, 0
    raise ValueError(f"unsupported release type: {release_type}")


def classify_one_step(
    previous: tuple[int, int, int], current: tuple[int, int, int]
) -> str | None:
    for release_type in ("patch", "minor", "major"):
        if current == expected_bump(previous, release_type):
            return release_type
    return None


def replace_property(text: str, key: str, value: str) -> str:
    lines = text.splitlines(keepends=True)
    matches = [index for index, line in enumerate(lines) if line.strip().startswith(f"{key}=")]
    if len(matches) != 1:
        raise ValueError(f"expected exactly one {key}= property, found {len(matches)}")
    index = matches[0]
    newline = "\n" if lines[index].endswith("\n") else ""
    lines[index] = f"{key}={value}{newline}"
    return "".join(lines)
