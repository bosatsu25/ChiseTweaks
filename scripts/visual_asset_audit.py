#!/usr/bin/env python3
"""Verify generated Kelp and Glass visual assets are present and structurally valid in the runtime JAR."""
from __future__ import annotations

import json
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIBS = ROOT / "build" / "libs"
CI = ROOT / "build" / "ci"

KELP_MODEL = "assets/chisetweaks/models/block/visual/kelp/party_overlay.json"
KELP_TEXTURE = "assets/chisetweaks/textures/block/visual/kelp/kelp_party.png"
KELP_ANIMATION = KELP_TEXTURE + ".mcmeta"
GLASS_BLOCK_MODEL = "assets/chisetweaks/models/block/visual/glass/block_overlay.json"
GLASS_PANE_MODEL = "assets/chisetweaks/models/block/visual/glass/pane_overlay.json"
GLASS_BLOCK_TEXTURE = "assets/chisetweaks/textures/block/visual/glass/glass_block_marker.png"
GLASS_PANE_TEXTURE = "assets/chisetweaks/textures/block/visual/glass/glass_pane_marker.png"
REQUIRED = (
    KELP_MODEL,
    KELP_TEXTURE,
    KELP_ANIMATION,
    GLASS_BLOCK_MODEL,
    GLASS_PANE_MODEL,
    GLASS_BLOCK_TEXTURE,
    GLASS_PANE_TEXTURE,
)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def runtime_jar() -> Path:
    candidates = sorted(path for path in LIBS.glob("*.jar") if not path.name.endswith("-sources.jar"))
    if len(candidates) != 1:
        raise ValueError(f"expected exactly one runtime JAR, found {len(candidates)}")
    return candidates[0]


def png_dimensions(data: bytes) -> tuple[int, int, int, int]:
    if len(data) < 33 or data[:8] != PNG_SIGNATURE or data[12:16] != b"IHDR":
        raise ValueError("invalid PNG header")
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, bit_depth, color_type


def load_json(archive: zipfile.ZipFile, path: str) -> dict:
    value = json.loads(archive.read(path).decode("utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path}: root must be an object")
    return value


def require_overlay_model(model: dict, path: str, texture_ref: str) -> None:
    if model.get("ambientocclusion") is not False:
        raise ValueError(f"{path}: ambientocclusion must be false")
    textures = model.get("textures")
    if not isinstance(textures, dict) or texture_ref not in textures.values():
        raise ValueError(f"{path}: expected texture reference {texture_ref}")
    elements = model.get("elements")
    if not isinstance(elements, list) or not elements:
        raise ValueError(f"{path}: elements must be non-empty")
    for index, element in enumerate(elements):
        if not isinstance(element, dict) or element.get("shade") is not False:
            raise ValueError(f"{path}: element {index} must set shade=false")


def main() -> int:
    try:
        jar = runtime_jar()
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
            missing = [path for path in REQUIRED if path not in names]
            if missing:
                raise ValueError("missing generated visual assets: " + ", ".join(missing))

            kelp_png = png_dimensions(archive.read(KELP_TEXTURE))
            block_png = png_dimensions(archive.read(GLASS_BLOCK_TEXTURE))
            pane_png = png_dimensions(archive.read(GLASS_PANE_TEXTURE))
            if kelp_png != (16, 128, 8, 6):
                raise ValueError(f"unexpected Kelp PNG IHDR: {kelp_png}")
            if block_png != (16, 16, 8, 6):
                raise ValueError(f"unexpected Glass block PNG IHDR: {block_png}")
            if pane_png != (16, 16, 8, 6):
                raise ValueError(f"unexpected Glass pane PNG IHDR: {pane_png}")

            animation = load_json(archive, KELP_ANIMATION).get("animation")
            if not isinstance(animation, dict):
                raise ValueError("Kelp animation metadata is missing animation object")
            if animation.get("frametime") != 3 or animation.get("interpolate") is not True:
                raise ValueError("Kelp animation metadata must keep frametime=3 and interpolate=true")

            require_overlay_model(
                load_json(archive, KELP_MODEL),
                KELP_MODEL,
                "chisetweaks:block/visual/kelp/kelp_party")
            require_overlay_model(
                load_json(archive, GLASS_BLOCK_MODEL),
                GLASS_BLOCK_MODEL,
                "chisetweaks:block/visual/glass/glass_block_marker")
            require_overlay_model(
                load_json(archive, GLASS_PANE_MODEL),
                GLASS_PANE_MODEL,
                "chisetweaks:block/visual/glass/glass_pane_marker")

        CI.mkdir(parents=True, exist_ok=True)
        report = {
            "runtime_jar": jar.name,
            "kelp": {"texture": KELP_TEXTURE, "dimensions": [16, 128], "frames": 8},
            "glass": {
                "block_texture": GLASS_BLOCK_TEXTURE,
                "pane_texture": GLASS_PANE_TEXTURE,
                "dimensions": [16, 16],
            },
            "required_asset_count": len(REQUIRED),
        }
        (CI / "visual-asset-audit.json").write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print("VISUAL ASSET AUDIT: PASS")
        print(f"runtime={jar.name}")
        print("kelp=16x128_rgba/8_frames")
        print("glass=2_models/2x16x16_rgba")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError, zipfile.BadZipFile) as error:
        print(f"VISUAL ASSET AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
