#!/usr/bin/env python3
"""Audit built ChiseTweaks artifacts and emit release-safe metadata."""
from __future__ import annotations

import hashlib
import json
import os
import struct
import sys
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build"
LIBS = BUILD / "libs"
CI_DIR = BUILD / "ci"

FORBIDDEN_ENTRY_TOKENS = (
    "PumpkinScaffold",
    "PlacementGuide",
    "mixin/sodium/",
    "LavaHighlightRendererMixin",
    "LavaFluidRenderHandler",
    "ExternalHookCircuitBreaker",
)

REQUIRED_LICENSE_ENTRIES = {
    "LICENSE_chise-tweaks",
    "LICENSE_MIT_chise-tweaks",
    "LICENSE_APACHE-2.0_chise-tweaks",
    "NOTICE",
}

# Release contract for Ore Highlights. These 11 semantic families cover every vanilla ore
# variant plus Ancient Debris. Obsidian and Crying Obsidian remain separate special materials.
ORE_HIGHLIGHT_KEYS = {
    "coal",
    "iron",
    "copper",
    "gold",
    "lapis",
    "redstone",
    "diamond",
    "emerald",
    "nether_gold",
    "nether_quartz",
    "ancient_debris",
}
SPECIAL_MATERIAL_HIGHLIGHT_KEYS = {"obsidian", "crying_obsidian"}
ALL_HIGHLIGHT_KEYS = ORE_HIGHLIGHT_KEYS | SPECIAL_MATERIAL_HIGHLIGHT_KEYS

MODEL_TO_HIGHLIGHT = {
    "coal_ore": "coal",
    "deepslate_coal_ore": "coal",
    "iron_ore": "iron",
    "deepslate_iron_ore": "iron",
    "copper_ore": "copper",
    "deepslate_copper_ore": "copper",
    "gold_ore": "gold",
    "deepslate_gold_ore": "gold",
    "lapis_ore": "lapis",
    "deepslate_lapis_ore": "lapis",
    "redstone_ore": "redstone",
    "deepslate_redstone_ore": "redstone",
    "diamond_ore": "diamond",
    "deepslate_diamond_ore": "diamond",
    "emerald_ore": "emerald",
    "deepslate_emerald_ore": "emerald",
    "nether_gold_ore": "nether_gold",
    "nether_quartz_ore": "nether_quartz",
    "ancient_debris": "ancient_debris",
    "obsidian": "obsidian",
    "crying_obsidian": "crying_obsidian",
}

VISUAL_TEXTURE_ROOT = "assets/chisetweaks/textures/block/visual/material/"
VISUAL_MODEL_ROOT = "assets/chisetweaks/models/block/visual/material/"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
CUBE_FACES = {"down", "up", "north", "south", "west", "east"}


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def duplicate_entries(names: list[str]) -> list[str]:
    seen: set[str] = set()
    duplicates: list[str] = []
    for name in names:
        if name in seen and name not in duplicates:
            duplicates.append(name)
        seen.add(name)
    return duplicates


def png_rgba_rows(data: bytes, path: str) -> tuple[int, int, list[bytes]]:
    """Decode the generated non-interlaced 8-bit RGBA PNG using only the Python stdlib."""
    if len(data) < 33 or not data.startswith(PNG_SIGNATURE):
        raise RuntimeError(f"Ore Highlights texture is not a valid PNG: {path}")

    offset = len(PNG_SIGNATURE)
    width = height = 0
    bit_depth = color_type = compression = filter_method = interlace = -1
    idat_parts: list[bytes] = []
    saw_ihdr = False

    while offset + 12 <= len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk_type = data[offset + 4:offset + 8]
        chunk_start = offset + 8
        chunk_end = chunk_start + length
        crc_end = chunk_end + 4
        if crc_end > len(data):
            raise RuntimeError(f"Ore Highlights PNG chunk is truncated: {path}")
        chunk = data[chunk_start:chunk_end]

        if chunk_type == b"IHDR":
            if saw_ihdr or length != 13:
                raise RuntimeError(f"Ore Highlights PNG has invalid IHDR: {path}")
            width, height, bit_depth, color_type, compression, filter_method, interlace = struct.unpack(
                ">IIBBBBB", chunk
            )
            saw_ihdr = True
        elif chunk_type == b"IDAT":
            idat_parts.append(chunk)
        elif chunk_type == b"IEND":
            break

        offset = crc_end

    if not saw_ihdr or not idat_parts:
        raise RuntimeError(f"Ore Highlights PNG is missing IHDR/IDAT: {path}")
    if (bit_depth, color_type, compression, filter_method, interlace) != (8, 6, 0, 0, 0):
        raise RuntimeError(
            "Ore Highlights texture must be non-interlaced 8-bit RGBA PNG: "
            f"{path} (bit_depth={bit_depth}, color_type={color_type}, "
            f"compression={compression}, filter={filter_method}, interlace={interlace})"
        )

    bytes_per_pixel = 4
    stride = width * bytes_per_pixel
    try:
        raw = zlib.decompress(b"".join(idat_parts))
    except zlib.error as error:
        raise RuntimeError(f"Ore Highlights PNG IDAT decode failed: {path}") from error
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise RuntimeError(
            f"Ore Highlights PNG decoded byte length mismatch: {path} ({len(raw)} != {expected})"
        )

    rows: list[bytes] = []
    previous = bytearray(stride)
    cursor = 0

    for _ in range(height):
        filter_type = raw[cursor]
        cursor += 1
        encoded = raw[cursor:cursor + stride]
        cursor += stride
        decoded = bytearray(stride)

        for index, value in enumerate(encoded):
            left = decoded[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            up = previous[index]
            upper_left = previous[index - bytes_per_pixel] if index >= bytes_per_pixel else 0

            if filter_type == 0:
                predictor = 0
            elif filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = up
            elif filter_type == 3:
                predictor = (left + up) // 2
            elif filter_type == 4:
                estimate = left + up - upper_left
                distance_left = abs(estimate - left)
                distance_up = abs(estimate - up)
                distance_upper_left = abs(estimate - upper_left)
                if distance_left <= distance_up and distance_left <= distance_upper_left:
                    predictor = left
                elif distance_up <= distance_upper_left:
                    predictor = up
                else:
                    predictor = upper_left
            else:
                raise RuntimeError(
                    f"Ore Highlights PNG uses unsupported filter {filter_type}: {path}"
                )

            decoded[index] = (value + predictor) & 0xFF

        rows.append(bytes(decoded))
        previous = decoded

    return width, height, rows


def alpha_mask_signature(rows: list[bytes], start_row: int, row_count: int) -> tuple[str, int, int]:
    selected = rows[start_row:start_row + row_count]
    alpha_values = [
        row[index]
        for row in selected
        for index in range(3, len(row), 4)
    ]
    mask = bytes(1 if alpha > 0 else 0 for alpha in alpha_values)
    visible = sum(mask)
    transparent = len(mask) - visible
    return hashlib.sha256(mask).hexdigest(), visible, transparent


def overlay_element(model: dict[str, object], model_id: str) -> dict[str, object]:
    for element in model.get("elements", []):
        start = element.get("from", [])
        end = element.get("to", [])
        if len(start) != 3 or len(end) != 3:
            continue
        if any(float(value) < 0.0 for value in start) or any(float(value) > 16.0 for value in end):
            return element
    raise RuntimeError(f"Ore Highlights model has no expanded overlay geometry: {model_id}")


def audit_ore_highlights(archive: zipfile.ZipFile, names: list[str]) -> dict[str, object]:
    name_set = set(names)

    # Chise must not silently become a shader/resource-pack distributor or mutate their contents.
    bundled_packs = sorted(
        name for name in names if name.startswith("shaderpacks/") or name.startswith("resourcepacks/")
    )
    if bundled_packs:
        raise RuntimeError(f"runtime JAR unexpectedly bundles shader/resource packs: {bundled_packs[:5]}")

    expected_pngs = {
        f"{VISUAL_TEXTURE_ROOT}{key}_highlight.png" for key in ALL_HIGHLIGHT_KEYS
    }
    expected_mcmeta = {f"{path}.mcmeta" for path in expected_pngs}
    expected_models = {
        f"{VISUAL_MODEL_ROOT}{model_id}.json" for model_id in MODEL_TO_HIGHLIGHT
    }

    actual_pngs = {
        name for name in names if name.startswith(VISUAL_TEXTURE_ROOT) and name.endswith("_highlight.png")
    }
    actual_mcmeta = {
        name
        for name in names
        if name.startswith(VISUAL_TEXTURE_ROOT) and name.endswith("_highlight.png.mcmeta")
    }
    actual_models = {
        name for name in names if name.startswith(VISUAL_MODEL_ROOT) and name.endswith(".json")
    }

    if actual_pngs != expected_pngs:
        raise RuntimeError(
            "Ore Highlights texture set differs from release contract; "
            f"missing={sorted(expected_pngs - actual_pngs)}, extra={sorted(actual_pngs - expected_pngs)}"
        )
    if actual_mcmeta != expected_mcmeta:
        raise RuntimeError(
            "Ore Highlights animation metadata set differs from release contract; "
            f"missing={sorted(expected_mcmeta - actual_mcmeta)}, extra={sorted(actual_mcmeta - expected_mcmeta)}"
        )
    if actual_models != expected_models:
        raise RuntimeError(
            "Ore Highlights model set differs from release contract; "
            f"missing={sorted(expected_models - actual_models)}, extra={sorted(actual_models - expected_models)}"
        )

    frame_zero_alpha_signatures: dict[str, str] = {}

    for key in sorted(ALL_HIGHLIGHT_KEYS):
        texture_path = f"{VISUAL_TEXTURE_ROOT}{key}_highlight.png"
        meta_path = f"{texture_path}.mcmeta"
        if texture_path not in name_set or meta_path not in name_set:
            raise RuntimeError(f"Ore Highlights asset pair missing for {key}")

        width, height, rows = png_rgba_rows(archive.read(texture_path), texture_path)
        if (width, height) != (16, 128):
            raise RuntimeError(
                f"Ore Highlights texture {key} must be 16x128 (8 x 16px frames), got {width}x{height}"
            )

        for frame in range(8):
            signature, visible, transparent = alpha_mask_signature(rows, frame * 16, 16)
            if visible == 0:
                raise RuntimeError(f"Ore Highlights frame is fully transparent: {key} frame {frame}")
            if transparent == 0:
                raise RuntimeError(
                    f"Ore Highlights frame is fully opaque and would hide the base texture: {key} frame {frame}"
                )
            if frame == 0:
                frame_zero_alpha_signatures[key] = signature

        metadata = json.loads(archive.read(meta_path).decode("utf-8"))
        animation = metadata.get("animation", {})
        if animation.get("frametime") != 1:
            raise RuntimeError(f"Ore Highlights animation frametime changed for {key}")
        if animation.get("interpolate") is not True:
            raise RuntimeError(f"Ore Highlights animation interpolation must remain enabled for {key}")

    # Accessibility invariant: after throwing away RGB entirely, every material still has a
    # different first-frame alpha silhouette. This proves the generated artifact is not color-only;
    # human readability under a particular display/shader remains a runtime visual-QA question.
    if len(set(frame_zero_alpha_signatures.values())) != len(ALL_HIGHLIGHT_KEYS):
        groups: dict[str, list[str]] = {}
        for key, signature in frame_zero_alpha_signatures.items():
            groups.setdefault(signature, []).append(key)
        duplicates = sorted(group for group in groups.values() if len(group) > 1)
        raise RuntimeError(
            f"Ore Highlights contains color-only duplicate alpha patterns: {duplicates}"
        )

    for model_id, highlight_key in MODEL_TO_HIGHLIGHT.items():
        model_path = f"{VISUAL_MODEL_ROOT}{model_id}.json"
        model = json.loads(archive.read(model_path).decode("utf-8"))
        if model.get("ambientocclusion") is not True:
            raise RuntimeError(f"Ore Highlights base model must keep ambient occlusion enabled: {model_id}")

        textures = model.get("textures", {})
        expected_highlight = f"chisetweaks:block/visual/material/{highlight_key}_highlight"
        if textures.get("highlight") != expected_highlight:
            raise RuntimeError(
                f"Ore Highlights model {model_id} points at unexpected highlight texture: {textures.get('highlight')!r}"
            )

        if model_id == "ancient_debris":
            if textures.get("top") != "minecraft:block/ancient_debris_top":
                raise RuntimeError("Ancient Debris top texture no longer delegates to the active Minecraft resource")
            if textures.get("side") != "minecraft:block/ancient_debris_side":
                raise RuntimeError("Ancient Debris side texture no longer delegates to the active Minecraft resource")
            if textures.get("particle") != "minecraft:block/ancient_debris_side":
                raise RuntimeError("Ancient Debris particle texture no longer delegates to the active Minecraft resource")
        else:
            expected_base = f"minecraft:block/{model_id}"
            if textures.get("base") != expected_base or textures.get("particle") != expected_base:
                raise RuntimeError(
                    f"Ore Highlights model {model_id} must preserve the Minecraft/resource-pack base texture reference"
                )

        overlay = overlay_element(model, model_id)
        if overlay.get("shade") is not False:
            raise RuntimeError(f"Ore Highlights overlay must disable vanilla element shading: {model_id}")
        faces = overlay.get("faces", {})
        if set(faces) != CUBE_FACES:
            raise RuntimeError(f"Ore Highlights overlay must cover exactly six visible block faces: {model_id}")
        for side, face in faces.items():
            if face.get("texture") != "#highlight" or face.get("cullface") != side:
                raise RuntimeError(f"Ore Highlights overlay face contract changed: {model_id}/{side}")

    return {
        "ore_families": len(ORE_HIGHLIGHT_KEYS),
        "ore_block_variants": 19,
        "special_materials": len(SPECIAL_MATERIAL_HIGHLIGHT_KEYS),
        "highlight_textures": len(expected_pngs),
        "highlight_models": len(expected_models),
        "animation_frames_per_texture": 8,
        "alpha_shape_signatures_unique": len(frame_zero_alpha_signatures),
        "all_frames_preserve_transparent_base_pixels": True,
        "resource_pack_base_texture_references_preserved": True,
        "shader_or_resource_packs_bundled": False,
    }


def audit_runtime(path: Path, expected_version: str, properties: dict[str, str]) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        duplicates = duplicate_entries(names)
        if duplicates:
            raise RuntimeError(f"runtime JAR contains duplicate entries: {duplicates[:5]}")
        for token in FORBIDDEN_ENTRY_TOKENS:
            if any(token in name for name in names):
                raise RuntimeError(f"runtime JAR contains removed production entry token: {token}")
        if any(name.endswith("Test.class") or "/test/" in name.lower() for name in names):
            raise RuntimeError("runtime JAR contains test classes")
        missing_licenses = sorted(REQUIRED_LICENSE_ENTRIES.difference(names))
        if missing_licenses:
            raise RuntimeError(f"runtime JAR missing license/notice entries: {missing_licenses}")
        if "fabric.mod.json" not in names:
            raise RuntimeError("runtime JAR is missing fabric.mod.json")

        metadata = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
        if metadata.get("id") != "chisetweaks":
            raise RuntimeError("fabric.mod.json id is not chisetweaks")
        if metadata.get("version") != expected_version:
            raise RuntimeError(
                f"fabric.mod.json version {metadata.get('version')!r} != {expected_version!r}"
            )
        if metadata.get("environment") != "client":
            raise RuntimeError("fabric.mod.json environment is not client")
        if set(metadata.get("entrypoints", {})) != {"client", "modmenu"}:
            raise RuntimeError("unexpected runtime entrypoint set")
        if metadata.get("mixins") != ["chisetweaks.features.mixins.json"]:
            raise RuntimeError("unexpected runtime mixin configuration set")

        depends = metadata.get("depends", {})
        if depends.get("minecraft") != properties["minecraft_version"]:
            raise RuntimeError("runtime Minecraft dependency does not match gradle.properties")
        if depends.get("java") != ">=25":
            raise RuntimeError("runtime Java dependency must be >=25")
        if "iris" in depends or "irisshaders" in depends:
            raise RuntimeError("Ore Highlights shader compatibility must not add a hard Iris dependency")

        custom = metadata.get("custom", {}).get("chisetweaks", {})
        if custom.get("side") != "client-only":
            raise RuntimeError("runtime custom side metadata is not client-only")
        for key in (
            "serverInstallationRequired",
            "customPlayProtocol",
            "remoteModDetection",
            "backgroundThreads",
            "automaticModDownload",
            "automaticJarReplacement",
            "modMenuRequired",
        ):
            if custom.get(key) is not False:
                raise RuntimeError(f"runtime custom metadata {key} must be false")

        if "chisetweaks.sodium.mixins.json" in names:
            raise RuntimeError("obsolete Sodium mixin config returned to runtime JAR")

        ore_highlights = audit_ore_highlights(archive, names)
        return {
            "entries": len(names),
            "environment": metadata.get("environment"),
            "entrypoints": sorted(metadata.get("entrypoints", {}).keys()),
            "mixins": metadata.get("mixins", []),
            "ore_highlights": ore_highlights,
        }


def audit_sources(path: Path) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        duplicates = duplicate_entries(names)
        if duplicates:
            raise RuntimeError(f"sources JAR contains duplicate entries: {duplicates[:5]}")
        for token in FORBIDDEN_ENTRY_TOKENS:
            if any(token in name for name in names):
                raise RuntimeError(f"sources JAR contains removed production entry token: {token}")
        missing_licenses = sorted(REQUIRED_LICENSE_ENTRIES.difference(names))
        if missing_licenses:
            raise RuntimeError(f"sources JAR missing license/notice entries: {missing_licenses}")
        return {"entries": len(names)}


def github_output(key: str, value: str) -> None:
    output = os.environ.get("GITHUB_OUTPUT")
    if not output:
        return
    with Path(output).open("a", encoding="utf-8") as handle:
        handle.write(f"{key}={value}\n")


def main() -> int:
    try:
        properties = read_properties(ROOT / "gradle.properties")
        version = properties["mod_version"]
        base_name = properties["archives_base_name"]
        runtime_name = f"{base_name}-{version}.jar"
        sources_name = f"{base_name}-{version}-sources.jar"
        runtime = LIBS / runtime_name
        sources = LIBS / sources_name
        if not runtime.is_file():
            raise RuntimeError(f"runtime JAR missing: {runtime}")
        if not sources.is_file():
            raise RuntimeError(f"sources JAR missing: {sources}")

        runtime_details = audit_runtime(runtime, version, properties)
        source_details = audit_sources(sources)
        runtime_hash = sha256(runtime)
        sources_hash = sha256(sources)

        CI_DIR.mkdir(parents=True, exist_ok=True)
        audit = {
            "version": version,
            "minecraft_version": properties["minecraft_version"],
            "java": 25,
            "runtime_jar": runtime_name,
            "runtime_sha256": runtime_hash,
            "sources_jar": sources_name,
            "sources_sha256": sources_hash,
            "client_only": True,
            "server_installation_required": False,
            "runtime": runtime_details,
            "sources": source_details,
        }
        (CI_DIR / "artifact-audit.json").write_text(
            json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        (CI_DIR / "SHA256SUMS.txt").write_text(
            f"{runtime_hash}  {runtime_name}\n{sources_hash}  {sources_name}\n",
            encoding="utf-8")
        (CI_DIR / "artifact-summary.md").write_text(
            "\n".join((
                "## Verified ChiseTweaks artifacts",
                "",
                f"- Version: `{version}`",
                f"- Minecraft: `{properties['minecraft_version']}`",
                "- Java: `25`",
                "- Side: **client-only**",
                f"- Runtime: `{runtime_name}`",
                f"- Runtime SHA-256: `{runtime_hash}`",
                f"- Sources: `{sources_name}`",
                f"- Sources SHA-256: `{sources_hash}`",
                "- Removed Pumpkin/Placement/Sodium-lava hook residue: **not present**",
                "- Ore Highlights vanilla coverage: **11 families / 19 block variants**",
                "- Ore Highlights generated assets: **13 animated RGBA overlays / 21 models**",
                "- Ore Highlights animation contract: **8 frames, frametime 1, interpolation on**",
                "- Ore Highlights color-independent artifact shapes: **13 / 13 unique alpha masks**",
                "- Ore Highlights frames preserve base visibility: **transparent pixels retained in every frame**",
                "- Ore Highlights base Minecraft/resource-pack texture identifiers: **preserved**",
                "- Bundled shaderpacks/resourcepacks: **none**",
                "- Hard Iris dependency: **none**",
                "",
            )),
            encoding="utf-8")

        github_output("version", version)
        github_output("runtime_jar", runtime_name)
        github_output("sources_jar", sources_name)
        github_output("runtime_sha256", runtime_hash)
        print("ARTIFACT AUDIT: PASS")
        print(f"runtime={runtime_name}")
        print(f"sha256={runtime_hash}")
        print("ore_highlights=11_families/19_variants/13_unique_alpha_patterns/21_models")
        return 0
    except (OSError, KeyError, ValueError, json.JSONDecodeError, zipfile.BadZipFile, zlib.error, RuntimeError) as error:
        print(f"ARTIFACT AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
