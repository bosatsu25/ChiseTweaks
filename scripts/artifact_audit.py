#!/usr/bin/env python3
"""Run the artifact audit with texture-free Bright and runtime-size contracts."""
from __future__ import annotations

import sys

import artifact_audit_core as core

LEGACY_BRIGHT_RESOURCE_ROOTS = (
    "resourcepacks/chise_chest_visibility/",
    "resourcepacks/chise_white_concrete_visibility/",
)


def audit_bright_resource_pack_residue() -> None:
    properties = gradle_properties()
    version = properties["mod_version"]
    base_name = properties["archives_base_name"]
    runtime = core.ROOT / "build" / "libs" / f"{base_name}-{version}.jar"
    if not runtime.is_file():
        raise RuntimeError(f"runtime JAR is missing for Bright residue audit: {runtime.name}")

    with core.zipfile.ZipFile(runtime) as archive:
        names = archive.namelist()
        legacy = sorted(
            name
            for name in names
            if any(name.startswith(root) for root in LEGACY_BRIGHT_RESOURCE_ROOTS)
        )
        if legacy:
            raise RuntimeError(
                "legacy Bright resource-pack entries returned to runtime JAR: "
                f"{legacy[:8]}"
            )


def gradle_properties() -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in (core.ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def audit_runtime_size() -> tuple[int, int, int, int]:
    properties = gradle_properties()
    version = properties["mod_version"]
    base_name = properties["archives_base_name"]
    runtime = core.ROOT / "build" / "libs" / f"{base_name}-{version}.jar"
    if not runtime.is_file():
        raise RuntimeError(f"runtime JAR is missing for size audit: {runtime.name}")

    goal = int(properties["runtime_jar_target_bytes"])
    baseline = int(properties["runtime_jar_baseline_bytes"])
    max_growth = int(properties["runtime_jar_max_growth_bytes"])
    absolute_max = int(properties["runtime_jar_max_bytes"])
    effective_max = min(absolute_max, baseline + max_growth)
    size = runtime.stat().st_size
    if size > effective_max:
        raise RuntimeError(
            "runtime JAR size regression: "
            f"size={size}, baseline={baseline}, max_growth={max_growth}, "
            f"effective_max={effective_max}"
        )

    with core.zipfile.ZipFile(runtime) as archive:
        data_descriptor_entries = [
            info.filename for info in archive.infolist()
            if info.flag_bits & 0x08
        ]
        if data_descriptor_entries:
            raise RuntimeError(
                "runtime JAR still contains ZIP data descriptors: "
                f"{data_descriptor_entries[:5]}"
            )

        extra_field_entries = [
            info.filename for info in archive.infolist()
            if info.extra
        ]
        if extra_field_entries:
            raise RuntimeError(
                "runtime JAR still contains avoidable ZIP extra fields: "
                f"{extra_field_entries[:5]}"
            )

    return size, baseline, goal, effective_max


def update_summary(size: int, baseline: int, goal: int, effective_max: int) -> None:
    summary = core.CI_DIR / "artifact-summary.md"
    if not summary.is_file():
        return
    text = summary.read_text(encoding="utf-8")
    remaining = max(0, size - goal)
    reduction = baseline - size
    text += (
        "\n- Bright built-in resource packs: **retired / no runtime entries**\n"
        "- Bright rendering: **existing Minecraft models/textures + lighting-only transforms**\n"
        f"- Runtime JAR size: **{size} bytes**\n"
        f"- Frozen hard-size baseline: **{baseline} bytes**\n"
        f"- 350 KiB final goal: **<= {goal} bytes**\n"
        f"- Reduction from hard-size baseline: **{reduction} bytes**\n"
        f"- Remaining to goal: **{remaining} bytes**\n"
        f"- ZIP metadata compaction: **descriptor-free / extra-field-free**\n"
        f"- No-growth hard ceiling: **<= {effective_max} bytes**\n"
    )
    summary.write_text(text, encoding="utf-8")


def main() -> int:
    result = core.main()
    if result != 0:
        return result
    try:
        audit_bright_resource_pack_residue()
        size, baseline, goal, effective_max = audit_runtime_size()
    except (KeyError, ValueError, OSError, RuntimeError) as failure:
        print(f"ARTIFACT AUDIT DISTRIBUTION CONTRACT: FAIL: {failure}", file=sys.stderr)
        return 1
    update_summary(size, baseline, goal, effective_max)
    print("BRIGHT RESOURCE-PACK RESIDUE: PASS legacy_entries=false")
    print(
        "ARTIFACT SIZE CONTRACT: PASS "
        f"size={size} baseline={baseline} goal={goal} "
        f"remaining={max(0, size - goal)} effective_max={effective_max} "
        "zip_metadata=compact"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
