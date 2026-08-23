#!/usr/bin/env python3
"""Keep README compatibility metadata synchronized with release properties."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def properties() -> dict[str, str]:
    result: dict[str, str] = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def require(pattern: str, text: str, label: str, failures: list[str]) -> None:
    if re.search(pattern, text, flags=re.MULTILINE) is None:
        failures.append(label)


def main() -> int:
    failures: list[str] = []
    props = properties()
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    fabric = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))

    version = props["mod_version"]
    core_version = version.split("+", 1)[0]
    minecraft = props["minecraft_version"]
    loader = props["loader_version"]
    fabric_api = props["fabric_api_version"]

    require(
        rf"^\| ChiseTweaks \| `{re.escape(version)}` \|$",
        readme,
        f"README ChiseTweaks row must be {version}",
        failures,
    )
    require(
        rf"^\| Minecraft \| `{re.escape(minecraft)}` \|$",
        readme,
        f"README Minecraft row must be {minecraft}",
        failures,
    )
    require(
        rf"^\| Fabric Loader \| `{re.escape(loader)}` 以上 \|$",
        readme,
        f"README Fabric Loader row must be {loader} or newer",
        failures,
    )
    require(
        rf"^\| Fabric API \| `{re.escape(fabric_api)}` 以上 \|$",
        readme,
        f"README Fabric API row must be {fabric_api} or newer",
        failures,
    )
    require(
        r"^\| Java \| `25` 以上 \|$",
        readme,
        "README Java row must require Java 25 or newer",
        failures,
    )
    require(
        rf"^## {re.escape(core_version)} の主な更新$",
        readme,
        f"README must contain the {core_version} current-update section",
        failures,
    )
    require(
        rf"`chise-tweaks-{re.escape(version)}\.jar`",
        readme,
        "README must name the current runtime JAR exactly",
        failures,
    )

    if fabric.get("version") != "${version}":
        failures.append("fabric.mod.json version must remain ${version}")
    depends = fabric.get("depends", {})
    expected_depends = {
        "minecraft": "${minecraft_version}",
        "fabricloader": ">=${loader_version}",
        "fabric-api": ">=${fabric_api_version}",
        "java": ">=25",
    }
    for key, expected in expected_depends.items():
        if depends.get(key) != expected:
            failures.append(f"fabric.mod.json dependency {key} must be {expected}")

    if failures:
        print("DOCUMENTATION CONSISTENCY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("DOCUMENTATION CONSISTENCY AUDIT: PASS")
    print(f"version={version}")
    print(f"minecraft={minecraft}")
    print("metadata_placeholders=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
