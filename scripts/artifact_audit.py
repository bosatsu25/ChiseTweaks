#!/usr/bin/env python3
"""Run the existing artifact audit with exact allowlists for Chise visibility packs."""
from __future__ import annotations

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


def update_summary() -> None:
    summary = core.CI_DIR / "artifact-summary.md"
    if not summary.is_file():
        return
    text = summary.read_text(encoding="utf-8")
    text = text.replace(
        "- Bundled shaderpacks/resourcepacks: **none**",
        "- Built-in resource packs: **Chest Visibility + White Concrete Visibility / 8 files**\n"
        "- Unexpected shaderpacks/resourcepacks: **none**",
    )
    summary.write_text(text, encoding="utf-8")


def main() -> int:
    core.audit_ore_highlights = audit_ore_highlights_with_visibility_packs
    result = core.main()
    if result == 0:
        update_summary()
    return result


if __name__ == "__main__":
    raise SystemExit(main())
