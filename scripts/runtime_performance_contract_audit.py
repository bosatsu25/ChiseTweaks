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
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java"),
}
ANALYZERS = {
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java"),
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

    concrete = java_code_only((ROOT / "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java").read_text(encoding="utf-8"))
    replacement_start = concrete.find("private void emitReplacementOrBase")
    overlay_start = concrete.find("private void emitExtraModel")
    replacement = concrete[replacement_start:overlay_start]
    if replacement_start < 0 or overlay_start <= replacement_start:
        failures.append("Bright Concrete replacement boundary disappeared")
    else:
        if "replacement.emitQuads" not in replacement:
            failures.append("Bright Concrete must emit the replacement model directly")
        if "FullbrightOverlayEmission.emit" in replacement:
            failures.append("Bright Concrete must preserve normal Minecraft lighting")

    for relative in ANALYZERS:
        path = ROOT / relative
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

    policy = ROOT / "src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java"
    policy_text = java_code_only(policy.read_text(encoding="utf-8"))
    for marker in (
        "MAX_BOOTSTRAP_CHUNKS_PER_TICK = 64",
        "MAX_VALIDATION_CHUNKS_PER_TICK = 16",
        "MAX_TRACKED_CHUNKS = 4096",
        "MAX_DEBRIS_PER_CHUNK = 256",
    ):
        if marker not in policy_text:
            failures.append(f"AncientDebrisAnalyzerPolicy budget changed or disappeared: {marker}")

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
    print("bright_concrete_normal_lighting=true")
    print("reload_controllers_get_join_free=true")
    print("analyzers_force_chunk_load=false")
    print("analyzer_budgets=bounded")
    print("pattern_consistency_scan=bounded_loaded_chunks_only")
    print("detector_self_test=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
