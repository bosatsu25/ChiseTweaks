#!/usr/bin/env python3
"""Reject deterministic runtime patterns that can regress startup/reload/analyzer responsiveness."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "src/main/java"
TEXTURE_RELOAD_CONTROLLER = Path(
    "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java"
)
FULL_RELOAD_CONTROLLER = Path(
    "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightModelReload.java"
)
RELOAD_CONTROLLERS = {TEXTURE_RELOAD_CONTROLLER, FULL_RELOAD_CONTROLLER}
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

# These controller files intentionally reserve every method literally named get(...) or join(...)
# as forbidden syntax. This conservative rule avoids pretending a regex is a Java data-flow engine.
# The one existing synchronous state-holder read is removed as a complete Java identifier token;
# any prefixed/suffixed receiver or any other wait remains visible to the detector.
ALLOWED_EXACT_SYNCHRONOUS_READS = (
    re.compile(r"(?<![\w$])TERMINAL_RECOVERY\s*\.\s*get\s*\(\s*\)"),
)


def java_code_only(text: str) -> str:
    """Remove comments so API names in documentation cannot affect the audit."""
    return JAVA_LINE_COMMENT.sub("", JAVA_BLOCK_COMMENT.sub("", text))


def strict_reload_controller_code(text: str) -> str:
    """Return executable controller text after removing only explicitly approved synchronous reads."""
    code = java_code_only(text)
    for allowed in ALLOWED_EXACT_SYNCHRONOUS_READS:
        code = allowed.sub("", code)
    return code


def blocking_wait_syntax(text: str) -> list[str]:
    """Reject every remaining .get(...) or .join(...) inside the two reload controllers."""
    code = strict_reload_controller_code(text)
    findings: list[str] = []
    for match in BLOCKING_WAIT_SYNTAX.finditer(code):
        start = max(0, match.start() - 48)
        end = min(len(code), match.end() + 48)
        snippet = " ".join(code[start:end].split())
        findings.append(snippet)
    return findings


def detector_contract_failures() -> list[str]:
    """Self-test the intentionally conservative syntax contract against prior review regressions."""
    cases = (
        ("TERMINAL_RECOVERY.get();", False, "the one exact AtomicReference read remains allowed"),
        ("TERMINAL_RECOVERY . get ( );", False, "formatting around the exact allowed read remains allowed"),
        (
            "OTHER_TERMINAL_RECOVERY.get();",
            True,
            "a receiver merely ending with the allowed identifier must remain forbidden",
        ),
        (
            "TERMINAL_RECOVERY_EXTRA.get();",
            True,
            "a receiver extending the allowed identifier must remain forbidden",
        ),
        (
            "TERMINAL_RECOVERY.get(); future.join();",
            True,
            "the allowed read must not mask another wait on the same line",
        ),
        ("// future.get();", False, "comment-only wait examples must be ignored"),
        ("future.get();", True, "zero-argument get is forbidden in a reload controller"),
        ("future.get(5, TimeUnit.SECONDS);", True, "timed get is forbidden in a reload controller"),
        ("future.join();", True, "join is forbidden in a reload controller"),
        ("(future).join();", True, "parenthesized receiver waits are forbidden"),
        (
            "client.reloadResourcePacks()\n    .thenApply(v -> { record(v); return v; })\n    .join();",
            True,
            "block-bodied completion stages cannot hide a terminal wait",
        ),
        (
            "var reload = client.delayTextureReload();\nreload.get(5, TimeUnit.SECONDS);",
            True,
            "stored delayed texture reload Future timed waits are forbidden without data-flow analysis",
        ),
        (
            "consume(client.reloadResourcePacks(), cache.get(key));",
            True,
            "ordinary get calls are deliberately forbidden in the tiny reload-controller boundary",
        ),
        (
            "Supplier<CompletableFuture<Void>> reload = () -> client.delayTextureReload(); reload.get();",
            True,
            "Supplier.get is deliberately forbidden in the reload-controller boundary",
        ),
        (
            "void first() { var result = client.reloadResourcePacks(); } "
            "void second() { other.get(); }",
            True,
            "method-scope ambiguity cannot bypass the strict controller syntax rule",
        ),
    )
    failures: list[str] = []
    for sample, expected, label in cases:
        actual = bool(blocking_wait_syntax(sample))
        if actual != expected:
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
    if delayed_texture_reload_callers != {TEXTURE_RELOAD_CONTROLLER}:
        failures.append(
            "delayed texture reload callers changed; expected only the built-in visibility pack controller: "
            f"actual={sorted(map(str, delayed_texture_reload_callers))}"
        )

    texture = ROOT / TEXTURE_RELOAD_CONTROLLER
    texture_text = texture.read_text(encoding="utf-8")
    texture_code = java_code_only(texture_text)
    for marker in (
        "ResourceReloadCoordinator",
        "whenComplete",
        "client.execute",
        "markPending",
        "delayTextureReload",
    ):
        if marker not in texture_code:
            failures.append(f"{texture.relative_to(ROOT)}: missing non-blocking texture reload marker {marker}")
    if FULL_RELOAD_CALL.search(texture_code):
        failures.append(
            f"{texture.relative_to(ROOT)}: Bright built-in packs must not invoke the foreground full resource reload"
        )
    for wait in blocking_wait_syntax(texture_text):
        failures.append(f"{texture.relative_to(ROOT)}: forbidden get/join syntax in reload controller: {wait}")

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
    print("visibility_pack_reload=delayed_texture")
    print("reload_controllers_get_join_free=true")
    print("analyzers_force_chunk_load=false")
    print("analyzer_budgets=bounded")
    print("pattern_consistency_scan=bounded_loaded_chunks_only")
    print("detector_self_test=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
