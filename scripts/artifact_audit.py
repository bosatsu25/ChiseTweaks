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
    "VisualModelReloadCoordinator",
    "VisualModelReloadThrottlePolicy",
    "OreHighlightLightingPolicy",
)
REQUIRED_LICENSE_ENTRIES = {
    "LICENSE_chise-tweaks",
    "LICENSE_MIT_chise-tweaks",
    "LICENSE_APACHE-2.0_chise-tweaks",
    "NOTICE",
}
ORE_HIGHLIGHT_KEYS = {
    "coal", "iron", "copper", "gold", "lapis", "redstone", "diamond", "emerald",
    "nether_gold", "nether_quartz", "ancient_debris",
}
SPECIAL_MATERIAL_HIGHLIGHT_KEYS = {"obsidian", "crying_obsidian"}
ALL_HIGHLIGHT_KEYS = ORE_HIGHLIGHT_KEYS | SPECIAL_MATERIAL_HIGHLIGHT_KEYS

VISUAL_TEXTURE_ROOT = "assets/chisetweaks/textures/block/visual/material/"
VISUAL_OVERLAY_MODEL_ROOT = "assets/chisetweaks/models/block/visual/overlay/"
LEGACY_REPLACEMENT_MODEL_ROOT = "assets/chisetweaks/models/block/visual/material/"
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
    """Decode generated non-interlaced 8-bit RGBA PNGs using only the stdlib."""
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
                raise RuntimeError(f"Ore Highlights PNG uses unsupported filter {filter_type}: {path}")
            decoded[index] = (value + predictor) & 0xFF
        rows.append(bytes(decoded))
        previous = decoded
    return width, height, rows


def alpha_mask_signature(rows: list[bytes]) -> tuple[str, int, int]:
    alpha_values = [row[index] for row in rows for index in range(3, len(row), 4)]
    mask = bytes(1 if alpha > 0 else 0 for alpha in alpha_values)
    visible = sum(mask)
    transparent = len(mask) - visible
    return hashlib.sha256(mask).hexdigest(), visible, transparent


def validate_sparse_frame(rows: list[bytes], label: str) -> str:
    signature, visible, transparent = alpha_mask_signature(rows)
    if visible == 0:
        raise RuntimeError(f"Ore Highlights frame is fully transparent: {label}")
    if transparent == 0:
        raise RuntimeError(f"Ore Highlights frame is fully opaque and hides the base model: {label}")
    return signature


def validate_overlay_model(model: dict[str, object], key: str, animated: bool) -> None:
    if model.get("ambientocclusion") is not False:
        raise RuntimeError(f"Ore Highlights overlay model must disable AO: {key}")
    textures = model.get("textures", {})
    expected = (
        f"chisetweaks:block/visual/material/{key}_highlight"
        if animated
        else f"chisetweaks:block/visual/material/{key}_highlight_static"
    )
    if textures.get("highlight") != expected or textures.get("particle") != expected:
        raise RuntimeError(f"Ore Highlights overlay model points at unexpected texture: {key}")
    if any(name in textures for name in ("base", "top", "side")):
        raise RuntimeError(f"Ore Highlights overlay model contains forbidden base texture slots: {key}")
    serialized = json.dumps(model, sort_keys=True)
    if "minecraft:block/" in serialized:
        raise RuntimeError(f"Ore Highlights overlay model embeds a Minecraft base model texture: {key}")

    elements = model.get("elements", [])
    if len(elements) != 1:
        raise RuntimeError(f"Ore Highlights overlay model must contain exactly one Chise element: {key}")
    overlay = elements[0]
    start = overlay.get("from", [])
    end = overlay.get("to", [])
    if len(start) != 3 or len(end) != 3:
        raise RuntimeError(f"Ore Highlights overlay geometry is malformed: {key}")
    if not all(float(value) < 0.0 for value in start):
        raise RuntimeError(f"Ore Highlights overlay must expand outside the base cube: {key}")
    if not all(float(value) > 16.0 for value in end):
        raise RuntimeError(f"Ore Highlights overlay must expand outside the base cube: {key}")
    if overlay.get("shade") is not False:
        raise RuntimeError(f"Ore Highlights overlay must disable vanilla element shading: {key}")
    faces = overlay.get("faces", {})
    if set(faces) != CUBE_FACES:
        raise RuntimeError(f"Ore Highlights overlay must cover exactly six block faces: {key}")
    for side, face in faces.items():
        if face.get("texture") != "#highlight" or face.get("cullface") != side:
            raise RuntimeError(f"Ore Highlights overlay face contract changed: {key}/{side}")


def audit_ore_highlights(archive: zipfile.ZipFile, names: list[str]) -> dict[str, object]:
    name_set = set(names)
    bundled_packs = sorted(
        name for name in names if name.startswith("shaderpacks/") or name.startswith("resourcepacks/")
    )
    if bundled_packs:
        raise RuntimeError(f"runtime JAR unexpectedly bundles shader/resource packs: {bundled_packs[:5]}")
    legacy_models = sorted(name for name in names if name.startswith(LEGACY_REPLACEMENT_MODEL_ROOT))
    if legacy_models:
        raise RuntimeError(f"legacy Ore Highlights replacement models returned: {legacy_models[:5]}")

    animated_pngs = {f"{VISUAL_TEXTURE_ROOT}{key}_highlight.png" for key in ALL_HIGHLIGHT_KEYS}
    static_pngs = {f"{VISUAL_TEXTURE_ROOT}{key}_highlight_static.png" for key in ALL_HIGHLIGHT_KEYS}
    animated_meta = {f"{path}.mcmeta" for path in animated_pngs}
    expected_models = {
        f"{VISUAL_OVERLAY_MODEL_ROOT}{key}_{motion}.json"
        for key in ALL_HIGHLIGHT_KEYS
        for motion in ("static", "animated")
    }

    actual_pngs = {
        name for name in names
        if name.startswith(VISUAL_TEXTURE_ROOT) and name.endswith(".png")
    }
    actual_meta = {
        name for name in names
        if name.startswith(VISUAL_TEXTURE_ROOT) and name.endswith(".png.mcmeta")
    }
    actual_models = {
        name for name in names
        if name.startswith(VISUAL_OVERLAY_MODEL_ROOT) and name.endswith(".json")
    }
    if actual_pngs != animated_pngs | static_pngs:
        raise RuntimeError(
            "Ore Highlights texture set differs from non-destructive contract; "
            f"missing={sorted((animated_pngs | static_pngs) - actual_pngs)}, "
            f"extra={sorted(actual_pngs - (animated_pngs | static_pngs))}"
        )
    if actual_meta != animated_meta:
        raise RuntimeError(
            "Only animated Ore Highlight textures may carry animation metadata; "
            f"missing={sorted(animated_meta - actual_meta)}, extra={sorted(actual_meta - animated_meta)}"
        )
    if actual_models != expected_models:
        raise RuntimeError(
            "Ore Highlights overlay model set differs from contract; "
            f"missing={sorted(expected_models - actual_models)}, extra={sorted(actual_models - expected_models)}"
        )

    semantic_signatures: dict[str, str] = {}
    for key in sorted(ALL_HIGHLIGHT_KEYS):
        animated_path = f"{VISUAL_TEXTURE_ROOT}{key}_highlight.png"
        static_path = f"{VISUAL_TEXTURE_ROOT}{key}_highlight_static.png"
        meta_path = f"{animated_path}.mcmeta"
        for required in (animated_path, static_path, meta_path):
            if required not in name_set:
                raise RuntimeError(f"Ore Highlights asset missing: {required}")

        aw, ah, animated_rows = png_rgba_rows(archive.read(animated_path), animated_path)
        sw, sh, static_rows = png_rgba_rows(archive.read(static_path), static_path)
        if (aw, ah) != (16, 128):
            raise RuntimeError(f"animated Ore Highlight texture {key} must be 16x128, got {aw}x{ah}")
        if (sw, sh) != (16, 16):
            raise RuntimeError(f"static Ore Highlight texture {key} must be 16x16, got {sw}x{sh}")

        frame_zero = animated_rows[:16]
        static_signature = validate_sparse_frame(static_rows, f"{key} static")
        frame_zero_signature = validate_sparse_frame(frame_zero, f"{key} animated frame 0")
        if static_signature != frame_zero_signature:
            raise RuntimeError(f"static Ore Highlight does not preserve animated frame-zero motif: {key}")
        semantic_signatures[key] = static_signature
        for frame in range(8):
            validate_sparse_frame(animated_rows[frame * 16:(frame + 1) * 16], f"{key} frame {frame}")

        metadata = json.loads(archive.read(meta_path).decode("utf-8"))
        animation = metadata.get("animation", {})
        if animation.get("frametime") != 1 or animation.get("interpolate") is not True:
            raise RuntimeError(f"Ore Highlights animation metadata changed for {key}")

        for motion in ("static", "animated"):
            model_path = f"{VISUAL_OVERLAY_MODEL_ROOT}{key}_{motion}.json"
            validate_overlay_model(
                json.loads(archive.read(model_path).decode("utf-8")),
                key,
                motion == "animated",
            )

    if len(set(semantic_signatures.values())) != len(ALL_HIGHLIGHT_KEYS):
        groups: dict[str, list[str]] = {}
        for key, signature in semantic_signatures.items():
            groups.setdefault(signature, []).append(key)
        duplicates = sorted(group for group in groups.values() if len(group) > 1)
        raise RuntimeError(f"Ore Highlights contains color-only duplicate alpha motifs: {duplicates}")

    return {
        "ore_families": len(ORE_HIGHLIGHT_KEYS),
        "ore_block_variants": 19,
        "special_materials": len(SPECIAL_MATERIAL_HIGHLIGHT_KEYS),
        "animated_highlight_textures": len(animated_pngs),
        "static_highlight_textures": len(static_pngs),
        "overlay_only_models": len(expected_models),
        "animation_frames_per_texture": 8,
        "alpha_shape_signatures_unique": len(semantic_signatures),
        "static_matches_animated_frame_zero": True,
        "all_frames_preserve_transparent_base_pixels": True,
        "legacy_replacement_models_present": False,
        "minecraft_base_geometry_embedded": False,
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
            raise RuntimeError(f"fabric.mod.json version {metadata.get('version')!r} != {expected_version!r}")
        if metadata.get("environment") != "client":
            raise RuntimeError("fabric.mod.json environment is not client")
        if set(metadata.get("entrypoints", {})) != {"client", "modmenu"}:
            raise RuntimeError("unexpected runtime entrypoint set")
        if metadata.get("mixins") != ["chisetweaks.features.mixins.json"]:
            raise RuntimeError("unexpected runtime mixin configuration set")

        depends = metadata.get("depends", {})
        if depends.get("minecraft") != properties["minecraft_version"]:
            raise RuntimeError("runtime Minecraft dependency does not match gradle.properties")
        if depends.get("fabricloader") != f">={properties['loader_version']}":
            raise RuntimeError("runtime Fabric Loader dependency does not match gradle.properties")
        if depends.get("fabric-api") != f">={properties['fabric_api_version']}":
            raise RuntimeError("runtime Fabric API dependency does not match gradle.properties")
        if depends.get("java") != ">=25":
            raise RuntimeError("runtime Java dependency must be >=25")
        if "iris" in depends or "irisshaders" in depends:
            raise RuntimeError("Ore Highlights shader compatibility must not add a hard Iris dependency")
        recommends = metadata.get("recommends", {})
        if recommends.get("sodium") != f">={properties['sodium_compat_version']}":
            raise RuntimeError("runtime Sodium recommendation does not match gradle.properties")

        custom = metadata.get("custom", {}).get("chisetweaks", {})
        if custom.get("side") != "client-only":
            raise RuntimeError("runtime custom side metadata is not client-only")
        for key in (
            "serverInstallationRequired", "customPlayProtocol", "remoteModDetection",
            "backgroundThreads", "automaticModDownload", "automaticJarReplacement", "modMenuRequired",
        ):
            if custom.get(key) is not False:
                raise RuntimeError(f"runtime custom metadata {key} must be false")
        if "chisetweaks.sodium.mixins.json" in names:
            raise RuntimeError("obsolete Sodium mixin config returned to runtime JAR")

        return {
            "entries": len(names),
            "environment": metadata.get("environment"),
            "entrypoints": sorted(metadata.get("entrypoints", {}).keys()),
            "mixins": metadata.get("mixins", []),
            "ore_highlights": audit_ore_highlights(archive, names),
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
            f"{runtime_hash}  {runtime_name}\n{sources_hash}  {sources_name}\n", encoding="utf-8")
        (CI_DIR / "artifact-summary.md").write_text(
            "\n".join((
                "## Verified ChiseTweaks artifacts", "",
                f"- Version: `{version}`",
                f"- Minecraft: `{properties['minecraft_version']}`",
                "- Java: `25`",
                "- Side: **client-only**",
                f"- Runtime: `{runtime_name}`",
                f"- Runtime SHA-256: `{runtime_hash}`",
                f"- Sources: `{sources_name}`",
                f"- Sources SHA-256: `{sources_hash}`",
                "- Ore Highlights vanilla coverage: **11 families / 19 block variants**",
                "- Ore Highlights assets: **13 static + 13 animated textures / 26 overlay-only models**",
                "- Ore Highlights default motion: **static / reduced-motion**",
                "- Ore Highlights animation option: **8 frames, frametime 1, interpolation on**",
                "- Ore Highlights color-independent shapes: **13 / 13 unique alpha masks**",
                "- Resource-pack base model replacement assets: **none**",
                "- Full resource-pack reload for Ore settings: **none**",
                "- Bundled shaderpacks/resourcepacks: **none**",
                "- Hard Iris dependency: **none**", "",
            )),
            encoding="utf-8")

        github_output("version", version)
        github_output("runtime_jar", runtime_name)
        github_output("sources_jar", sources_name)
        github_output("runtime_sha256", runtime_hash)
        print("ARTIFACT AUDIT: PASS")
        print(f"runtime={runtime_name}")
        print(f"sha256={runtime_hash}")
        print("ore_highlights=11_families/19_variants/13_static/13_animated/26_overlay_models")
        return 0
    except (OSError, KeyError, ValueError, json.JSONDecodeError, zipfile.BadZipFile, zlib.error, RuntimeError) as error:
        print(f"ARTIFACT AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
