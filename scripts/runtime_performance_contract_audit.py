#!/usr/bin/env python3
"""Reject deterministic runtime patterns that can regress startup/reload/analyzer responsiveness."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "src/main/java"
FULL_RELOAD_CONTROLLER = Path(
    "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightModelReload.java"
)
BRIGHT_RENDERING_PATHS = {
    Path("src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java"),
    Path("src/main/java/dev/chise/chisetweaks/mixin/rendering/ChestVisibilityMixin.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java"),
}
ANALYZERS = {
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java"),
}
PATTERN_INSPECTOR = Path(
    "src/main/java/dev/chise/chisetweaks/gui/PatternConsistencyInspector.java"
)
FULL_RELOAD_CALL = re.compile(r"\breloadResourcePacks\s*\(\s*\)")
DELAYED_TEXTURE_RELOAD_CALL = re.compile(r"\bdelayTextureReload\s*\(\s*\)")
BLOCKING_WAIT_SYNTAX = re.compile(r"\.\s*(?:join|get)\s*\(")
PLAIN_GET_CHUNK = re.compile(r"\bgetChunk\s*\(")
JAVA_BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
JAVA_LINE_COMMENT = re.compile(r"//.*?$", re.MULTILINE)

ALLOWED_EXACT_SYNCHRONOUS_READS = (
    re.compile(r"(?<![\w$])TERMINAL_RECOVERY\s*\.\s*get\s*\(\s*\)"),
)


def java_code_only(text: str) -> str:
    return JAVA_LINE_COMMENT.sub("", JAVA_BLOCK_COMMENT.sub("", text))


def strict_reload_controller_code(text: str) -> str:
    code = java_code_only(text)
    for allowed in ALLOWED_EXACT_SYNCHRONOUS_READS:
        code = allowed.sub("", code)
    return code


def blocking_wait_syntax(text: str) -> list[str]:
    code = strict_reload_controller_code(text)
    findings: list[str] = []
    for match in BLOCKING_WAIT_SYNTAX.finditer(code):
        start = max(0, match.start() - 48)
        end = min(len(code), match.end() + 48)
        findings.append(" ".join(code[start:end].split()))
    return findings


def detector_contract_failures() -> list[str]:
    cases = (
        ("TERMINAL_RECOVERY.get();", False, "the one exact AtomicReference read remains allowed"),
        ("TERMINAL_RECOVERY . get ( );", False, "formatting around the exact allowed read remains allowed"),
        ("OTHER_TERMINAL_RECOVERY.get();", True, "suffix lookalike remains forbidden"),
        ("TERMINAL_RECOVERY_EXTRA.get();", True, "extended identifier remains forbidden"),
        ("TERMINAL_RECOVERY.get(); future.join();", True, "another wait remains visible"),
        ("// future.get();", False, "comment-only wait examples are ignored"),
        ("future.get();", True, "zero-argument get is forbidden"),
        ("future.get(5, TimeUnit.SECONDS);", True, "timed get is forbidden"),
        ("future.join();", True, "join is forbidden"),
        ("(future).join();", True, "parenthesized wait is forbidden"),
    )
    failures: list[str] = []
    for sample, expected, label in cases:
        if bool(blocking_wait_syntax(sample)) != expected:
            failures.append(f"blocking-wait detector self-test failed: {label}: sample={sample!r}")
    return failures


def audit() -> list[str]:
    failures = detector_contract_failures()
    full_reload_callers: set[Path] = set()
    delayed_texture_reload_callers: set[Path] = set()

    for path in PRODUCTION.rglob("*.java"):
        relative = path.relative_to(ROOT)
        text = java_code_only(path.read_text(encoding="utf-8"))
        if "Thread.sleep(" in text:
            failures.append(f"{relative}: production thread sleep is forbidden")
        if FULL_RELOAD_CALL.search(text):
            full_reload_callers.add(relative)
        if DELAYED_TEXTURE_RELOAD_CALL.search(text):
            delayed_texture_reload_callers.add(relative)

    if full_reload_callers != {FULL_RELOAD_CONTROLLER}:
        failures.append(
            "full resource reload callers changed; expected only the model reload controller: "
            f"actual={sorted(map(str, full_reload_callers))}"
        )
    if delayed_texture_reload_callers:
        failures.append(
            "delayed texture reload callers returned after Bright pack retirement: "
            f"actual={sorted(map(str, delayed_texture_reload_callers))}"
        )

    ore = ROOT / FULL_RELOAD_CONTROLLER
    ore_text = ore.read_text(encoding="utf-8")
    ore_code = java_code_only(ore_text)
    for marker in ("AtomicBoolean", "whenComplete", "client.execute", "PENDING", "reloadResourcePacks"):
        if marker not in ore_code:
            failures.append(f"{ore.relative_to(ROOT)}: missing coalescing full reload marker {marker}")
    if DELAYED_TEXTURE_RELOAD_CALL.search(ore_code):
        failures.append(f"{ore.relative_to(ROOT)}: model rebuild must not silently downgrade to texture-only reload")
    for wait in blocking_wait_syntax(ore_text):
        failures.append(f"{ore.relative_to(ROOT)}: forbidden get/join syntax in reload controller: {wait}")

    for relative in BRIGHT_RENDERING_PATHS:
        path = ROOT / relative
        if not path.is_file():
            failures.append(f"{relative}: required direct Bright rendering boundary is missing")
            continue
        code = java_code_only(path.read_text(encoding="utf-8"))
        if FULL_RELOAD_CALL.search(code) or DELAYED_TEXTURE_RELOAD_CALL.search(code):
            failures.append(f"{relative}: Bright rendering must not trigger resource reload")
        if blocking_wait_syntax(code):
            failures.append(f"{relative}: Bright rendering must not block on get/join")

    chest_state_path = ROOT / "src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java"
    chest_state = java_code_only(chest_state_path.read_text(encoding="utf-8"))
    for marker in (
        "FeatureSwitches.BRIGHT_CHEST.getBooleanValue()",
        "state instanceof ChestRenderState chest",
        "chest.lightCoords = LightCoordsUtil.FULL_BRIGHT",
    ):
        if marker not in chest_state:
            failures.append(f"Bright Chest lightweight lighting boundary changed or disappeared: {marker}")
    for forbidden in ("customSprite", "SpriteId", "CHEST_MAPPER", "entity/chest/"):
        if forbidden in chest_state:
            failures.append(f"Bright Chest texture selection leaked into BlockEntity state extraction: {forbidden}")

    chest_sprite_path = ROOT / "src/main/java/dev/chise/chisetweaks/mixin/rendering/ChestVisibilityMixin.java"
    chest_sprite = java_code_only(chest_sprite_path.read_text(encoding="utf-8"))
    for marker in (
        "Sheets.CHEST_MAPPER.apply",
        'Identifier.fromNamespaceAndPath("chisetweaks", "normal")',
        'Identifier.fromNamespaceAndPath("chisetweaks", "normal_left")',
        'Identifier.fromNamespaceAndPath("chisetweaks", "normal_right")',
        "case LEFT -> CHISETWEAKS$BRIGHT_LEFT",
        "case RIGHT -> CHISETWEAKS$BRIGHT_RIGHT",
    ):
        if marker not in chest_sprite:
            failures.append(f"Bright Chest dedicated sprite boundary changed or disappeared: {marker}")
    for forbidden in ("Sheets.BLOCKS_MAPPER.apply", "white_concrete", "reloadResourcePacks", "delayTextureReload"):
        if forbidden in chest_sprite:
            failures.append(f"Bright Chest must stay on the dedicated CHEST-atlas path: {forbidden}")
    for relative in (
        "src/main/resources/assets/chisetweaks/textures/entity/chest/normal.png",
        "src/main/resources/assets/chisetweaks/textures/entity/chest/normal_left.png",
        "src/main/resources/assets/chisetweaks/textures/entity/chest/normal_right.png",
    ):
        if not (ROOT / relative).is_file():
            failures.append(f"Bright Chest dedicated texture missing: {relative}")

    concrete_path = ROOT / "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java"
    concrete = java_code_only(concrete_path.read_text(encoding="utf-8"))
    bright_start = concrete.find("private void emitBrightConcrete")
    overlay_start = concrete.find("private void emitExtraModel")
    bright = concrete[bright_start:overlay_start]
    if bright_start < 0 or overlay_start <= bright_start:
        failures.append("Bright Concrete lightweight lighting boundary disappeared")
    else:
        for marker in ("emitter.pushTransform", "FullbrightOverlayLighting.apply(quad)", "super.emitQuads", "emitter.popTransform"):
            if marker not in bright:
                failures.append(f"Bright Concrete must transform the vanilla model in-place: {marker}")
        for forbidden in ("overlayModel(", "replacement.emitQuads", "FullbrightOverlayEmission.emit"):
            if forbidden in bright:
                failures.append(f"Bright Concrete must not emit a replacement/extra model: {forbidden}")

    plugin = java_code_only((ROOT / "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java").read_text(encoding="utf-8"))
    if "FullbrightOverlayModel.brightConcrete(model)" not in plugin:
        failures.append("Bright Concrete must wrap the vanilla White Concrete model directly")
    for forbidden in ("BRIGHT_CONCRETE_MODEL", "BRIGHT_CONCRETE_KEY", "block/visual/bright_concrete"):
        if forbidden in plugin:
            failures.append(f"Bright Concrete custom model dependency returned: {forbidden}")

    for relative in ANALYZERS:
        path = ROOT / relative
        if not path.is_file():
            failures.append(f"{relative}: retained analyzer implementation is missing")
            continue
        text = java_code_only(path.read_text(encoding="utf-8"))
        if "getChunkNow(" not in text:
            failures.append(f"{relative}: analyzer must inspect only already-loaded chunks")
        if PLAIN_GET_CHUNK.search(text):
            failures.append(f"{relative}: potentially force-loading getChunk call detected")

    pattern_text = java_code_only((ROOT / PATTERN_INSPECTOR).read_text(encoding="utf-8"))
    for marker in (
        "HORIZONTAL_RADIUS = 8",
        "VERTICAL_RADIUS = 4",
        "MAX_BLOCKS_PER_TICK = 256",
        "MAX_RETAINED_MISMATCHES = 64",
        "RESCAN_INTERVAL_TICKS = 20",
        "processed++ < MAX_BLOCKS_PER_TICK",
        "getChunkSource().hasChunk",
    ):
        if marker not in pattern_text:
            failures.append(f"Pattern Consistency budget changed or disappeared: {marker}")
    if PLAIN_GET_CHUNK.search(pattern_text):
        failures.append(f"{PATTERN_INSPECTOR}: potentially force-loading getChunk call detected")

    return failures


def main() -> int:
    failures = audit()
    if failures:
        print("RUNTIME PERFORMANCE CONTRACT AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1
    print("RUNTIME PERFORMANCE CONTRACT AUDIT: PASS")
    print("blocking_reload_waits=false")
    print("full_resource_reload_callers=1_model_controller")
    print("visibility_pack_reload=removed")
    print("bright_rendering_resource_reload=false")
    print("bright_chest_assets=3_dedicated_chest_textures")
    print("bright_chest=chest_atlas_sprite_selection_plus_fullbright_lightcoords")
    print("bright_concrete=vanilla_model_fullbright_quad_transform")
    print("reload_controllers_get_join_free=true")
    print("analyzer_force_chunk_load=false")
    print(f"retained_analyzers={len(ANALYZERS)}")
    print("pattern_consistency_scan=bounded_loaded_chunks_only")
    print("detector_self_test=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
