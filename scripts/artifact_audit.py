#!/usr/bin/env python3
"""Run the artifact audit with exact visibility-pack and runtime-size contracts."""
from __future__ import annotations

import sys

import artifact_audit_core as core

CHEST_PACK_ROOT = "resourcepacks/chise_chest_visibility/"
WHITE_CONCRETE_PACK_ROOT = "resourcepacks/chise_white_concrete_visibility/"
VISIBILITY_PACK_FILES = {
    f"{CHEST_PACK_ROOT}pack.mcmeta",
    f"{CHEST_PACK_ROOT}pack.png",
    f"{CHEST_PACK_ROOT}assets/minecraft/textures/entity/chest/normal.png",
    f"{CHEST_PACK_ROOT}assets/minecraft/textures/entity/chest/normal_left.png",
    f"{CHEST_PACK_ROOT}assets/minecraft/textures/entity/chest/normal_right.png",
    f"{WHITE_CONCRETE_PACK_ROOT}pack.mcmeta",
    f"{WHITE_CONCRETE_PACK_ROOT}pack.png",
    f"{WHITE_CONCRETE_PACK_ROOT}assets/minecraft/textures/block/white_concrete.png",
}

_ORIGINAL_AUDIT_ORE_HIGHLIGHTS = core.audit_ore_highlights


def audit_ore_highlights_with_visibility_packs(
        archive: core.zipfile.ZipFile,
        names: list[str]) -> dict[str, object]:
    shader_packs = sorted(name for name in names if name.startswith("shaderpacks/"))
    if shader_packs:
        raise RuntimeError(f"runtime JAR unexpectedly bundles shader packs: {shader_packs[:5]}")

    resource_pack_files = {
        name for name in names
        if name.startswith("resourcepacks/") and not name.endswith("/")
    }
    if resource_pack_files != VISIBILITY_PACK_FILES:
        raise RuntimeError(
            "runtime JAR resource-pack set differs from the split visibility-pack contract; "
            f"missing={sorted(VISIBILITY_PACK_FILES - resource_pack_files)}, "
            f"extra={sorted(resource_pack_files - VISIBILITY_PACK_FILES)}"
        )

    names_without_builtin_packs = [
        name for name in names if not name.startswith("resourcepacks/")
    ]
    details = _ORIGINAL_AUDIT_ORE_HIGHLIGHTS(archive, names_without_builtin_packs)
    details.pop("shader_or_resource_packs_bundled", None)
    details["unexpected_shader_or_resource_packs_bundled"] = False
    details["builtin_resource_packs"] = [
        "chise_chest_visibility",
        "chise_white_concrete_visibility",
    ]
    details["builtin_resource_pack_files"] = len(VISIBILITY_PACK_FILES)
    return details


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
    text = text.replace(
        "- Bundled shaderpacks/resourcepacks: **none**",
        "- Built-in resource packs: **Chest Visibility + White Concrete Visibility / 8 files**\n"
        "- Unexpected shaderpacks/resourcepacks: **none**",
    )
    remaining = max(0, size - goal)
    reduction = baseline - size
    text += (
        f"\n- Runtime JAR size: **{size} bytes**\n"
        f"- Frozen 0.9.4 functional baseline: **{baseline} bytes**\n"
        f"- 350 KiB final goal: **<= {goal} bytes**\n"
        f"- Reduction from baseline: **{reduction} bytes**\n"
        f"- Remaining to goal: **{remaining} bytes**\n"
        f"- ZIP metadata compaction: **descriptor-free / extra-field-free**\n"
        f"- No-growth hard ceiling: **<= {effective_max} bytes**\n"
    )
    summary.write_text(text, encoding="utf-8")


def main() -> int:
    core.audit_ore_highlights = audit_ore_highlights_with_visibility_packs
    result = core.main()
    if result != 0:
        return result
    try:
        size, baseline, goal, effective_max = audit_runtime_size()
    except (KeyError, ValueError, OSError, RuntimeError) as failure:
        print(f"ARTIFACT AUDIT SIZE CONTRACT: FAIL: {failure}", file=sys.stderr)
        return 1
    update_summary(size, baseline, goal, effective_max)
    print(
        "ARTIFACT SIZE CONTRACT: PASS "
        f"size={size} baseline={baseline} goal={goal} "
        f"remaining={max(0, size - goal)} effective_max={effective_max} "
        "zip_metadata=compact"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
