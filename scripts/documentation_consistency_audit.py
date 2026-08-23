#!/usr/bin/env python3
"""Keep README compatibility metadata and runtime-size policy synchronized with release properties."""
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
    jar_goal = props["runtime_jar_target_bytes"]
    jar_baseline = props["runtime_jar_baseline_bytes"]
    jar_growth = props["runtime_jar_max_growth_bytes"]
    jar_max = props["runtime_jar_max_bytes"]

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
    require(
        rf"^- runtime JAR最終目標: `{re.escape(jar_goal)} bytes` 以下（350 KiB）$",
        readme,
        f"README runtime JAR goal must be {jar_goal} bytes",
        failures,
    )
    require(
        rf"^- M0 frozen size baseline: `{re.escape(jar_baseline)} bytes`$",
        readme,
        f"README runtime JAR baseline must be {jar_baseline} bytes",
        failures,
    )
    require(
        rf"^- 軽量化中に許容する容量増加: `{re.escape(jar_growth)} bytes`$",
        readme,
        f"README runtime JAR growth allowance must be {jar_growth} bytes",
        failures,
    )
    require(
        rf"^- absolute / effective CI上限: `{re.escape(jar_max)} bytes`$",
        readme,
        f"README runtime JAR hard ceiling must be {jar_max} bytes",
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
    print(f"runtime_jar_goal={jar_goal}")
    print(f"runtime_jar_baseline={jar_baseline}")
    print(f"runtime_jar_growth={jar_growth}")
    print(f"runtime_jar_max={jar_max}")
    print("metadata_placeholders=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
