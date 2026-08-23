#!/usr/bin/env python3
"""Reject deterministic runtime patterns that can regress startup/reload/analyzer responsiveness."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "src/main/java"
RELOAD_CONTROLLERS = {
    Path("src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightModelReload.java"),
}
ANALYZERS = {
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"),
    Path("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java"),
}
RELOAD_CALL = re.compile(r"\breloadResourcePacks\s*\(\s*\)")
RELOAD_CHAIN_WAIT = re.compile(
    r"\breloadResourcePacks\s*\(\s*\)[^;]*?\.\s*(?:join|get)\s*\(",
)
SIMPLE_ASSIGNMENT = re.compile(
    r"(?P<lhs>(?:this\s*\.\s*)?[A-Za-z_$][\w$]*)\s*(?<![=!<>])=(?!=)\s*(?P<rhs>[^;]+);"
)
PLAIN_GET_CHUNK = re.compile(r"\bgetChunk\s*\(")
JAVA_BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
JAVA_LINE_COMMENT = re.compile(r"//.*?$", re.MULTILINE)
JAVA_WHITESPACE = re.compile(r"\s+")


def java_code_only(text: str) -> str:
    """Remove comments so examples of forbidden APIs do not become audit false positives."""
    return JAVA_LINE_COMMENT.sub("", JAVA_BLOCK_COMMENT.sub("", text))


def normalized_java(text: str) -> str:
    """Collapse formatting whitespace so multiline Java calls are inspected as one logical stream."""
    return JAVA_WHITESPACE.sub(" ", java_code_only(text)).strip()


def canonical_receiver(receiver: str) -> str:
    return JAVA_WHITESPACE.sub("", receiver)


def strip_outer_parentheses(expression: str) -> str:
    current = expression.strip()
    while len(current) >= 2 and current[0] == "(" and current[-1] == ")":
        current = current[1:-1].strip()
    return current


def reload_future_receivers(code: str) -> set[str]:
    """Find variables/fields that directly receive the reload Future and follow simple aliases."""
    assignments = [
        (canonical_receiver(match.group("lhs")), match.group("rhs").strip())
        for match in SIMPLE_ASSIGNMENT.finditer(code)
    ]
    receivers = {
        lhs for lhs, rhs in assignments if RELOAD_CALL.search(rhs)
    }

    changed = True
    while changed:
        changed = False
        for lhs, rhs in assignments:
            if lhs in receivers:
                continue
            alias = canonical_receiver(strip_outer_parentheses(rhs))
            if alias in receivers:
                receivers.add(lhs)
                changed = True
    return receivers


def receiver_wait_pattern(receiver: str) -> re.Pattern[str]:
    if receiver.startswith("this."):
        field = re.escape(receiver.removeprefix("this."))
        receiver_pattern = rf"(?:this\s*\.\s*)?{field}"
    else:
        receiver_pattern = re.escape(receiver)
    return re.compile(rf"(?<![\w$]){receiver_pattern}\s*\.\s*(?:join|get)\s*\(")


def blocking_reload_waits(text: str) -> list[str]:
    """Detect blocking waits only on reloadResourcePacks Futures, including stored/aliased Futures."""
    code = normalized_java(text)
    if not code:
        return []

    findings: list[str] = []
    for match in RELOAD_CHAIN_WAIT.finditer(code):
        findings.append(match.group(0))

    for receiver in sorted(reload_future_receivers(code)):
        pattern = receiver_wait_pattern(receiver)
        for match in pattern.finditer(code):
            findings.append(match.group(0))

    return list(dict.fromkeys(findings))


def detector_contract_failures() -> list[str]:
    """Self-test the detector against multiline, stored-Future, alias, masking and false-positive cases."""
    cases = (
        ("cache.get(key);", False, "ordinary collection get must not be treated as a reload wait"),
        ("TERMINAL_RECOVERY.get();", False, "unrelated AtomicReference get must remain allowed"),
        ("future.get();", False, "an unrelated Future name must not be assumed to be a reload Future"),
        ("// client.reloadResourcePacks().get();", False, "comment-only wait examples must be ignored"),
        (
            "client.reloadResourcePacks()\n    .get();",
            True,
            "multiline direct zero-argument reload get must be rejected",
        ),
        (
            "client.reloadResourcePacks()\n    .get(5, TimeUnit.SECONDS);",
            True,
            "multiline direct timed reload get must be rejected",
        ),
        (
            "client.reloadResourcePacks()\n    .thenApply(value -> value)\n    .join();",
            True,
            "multiline chained reload join must be rejected",
        ),
        (
            "var reload = client.reloadResourcePacks();\nreload.get(5, TimeUnit.SECONDS);",
            True,
            "timed get on a stored reload Future must be rejected",
        ),
        (
            "var reload = client.reloadResourcePacks();\nvar alias = reload;\nalias.join();",
            True,
            "blocking wait through a simple reload Future alias must be rejected",
        ),
        (
            "TERMINAL_RECOVERY.get(); client.reloadResourcePacks().join();",
            True,
            "an unrelated allowed get must not mask a blocking reload wait",
        ),
        (
            "var reload = client.reloadResourcePacks(); cache.get(reload);",
            False,
            "passing a reload Future to an unrelated collection get is not itself a blocking wait",
        ),
    )
    failures: list[str] = []
    for sample, expected, label in cases:
        actual = bool(blocking_reload_waits(sample))
        if actual != expected:
            failures.append(f"blocking-wait detector self-test failed: {label}: sample={sample!r}")
    return failures


def audit() -> list[str]:
    failures = detector_contract_failures()
    reload_callers: set[Path] = set()

    for path in PRODUCTION.rglob("*.java"):
        relative = path.relative_to(ROOT)
        text = java_code_only(path.read_text(encoding="utf-8"))
        if "Thread.sleep(" in text:
            failures.append(f"{relative}: production thread sleep is forbidden")
        if RELOAD_CALL.search(text):
            reload_callers.add(relative)

    if reload_callers != RELOAD_CONTROLLERS:
        failures.append(
            "resource reload callers changed; expected exactly the two coalescing controllers: "
            f"actual={sorted(map(str, reload_callers))}"
        )

    texture = ROOT / next(path for path in RELOAD_CONTROLLERS if "ChiseTexturePackController" in path.name)
    texture_text = texture.read_text(encoding="utf-8")
    for marker in ("ResourceReloadCoordinator", "whenComplete", "client.execute", "markPending"):
        if marker not in java_code_only(texture_text):
            failures.append(f"{texture.relative_to(ROOT)}: missing non-blocking reload marker {marker}")
    for wait in blocking_reload_waits(texture_text):
        failures.append(f"{texture.relative_to(ROOT)}: blocking reload wait detected: {wait}")

    ore = ROOT / next(path for path in RELOAD_CONTROLLERS if "OreHighlightModelReload" in path.name)
    ore_text = ore.read_text(encoding="utf-8")
    for marker in ("AtomicBoolean", "whenComplete", "client.execute", "PENDING"):
        if marker not in java_code_only(ore_text):
            failures.append(f"{ore.relative_to(ROOT)}: missing coalescing reload marker {marker}")
    for wait in blocking_reload_waits(ore_text):
        failures.append(f"{ore.relative_to(ROOT)}: blocking reload wait detected: {wait}")

    for relative in ANALYZERS:
        path = ROOT / relative
        text = java_code_only(path.read_text(encoding="utf-8"))
        if "getChunkNow(" not in text:
            failures.append(f"{relative}: analyzer must inspect only already-loaded chunks")
        if PLAIN_GET_CHUNK.search(text):
            failures.append(f"{relative}: potentially force-loading getChunk call detected")

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
    print("resource_reload_callers=2_coalescing_controllers")
    print("analyzers_force_chunk_load=false")
    print("analyzer_budgets=bounded")
    print("detector_self_test=true")
    print("reload_future_taint_tracking=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
