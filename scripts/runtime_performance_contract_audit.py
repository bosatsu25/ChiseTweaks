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
BLOCKING_ZERO_ARG_WAIT = re.compile(r"\.(?:join|get)\s*\(\s*\)")
DIRECT_RELOAD_TIMED_GET = re.compile(r"reloadResourcePacks\s*\(\s*\)\s*\.\s*get\s*\(")
PLAIN_GET_CHUNK = re.compile(r"\bgetChunk\s*\(")
JAVA_BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
JAVA_LINE_COMMENT = re.compile(r"//.*?$", re.MULTILINE)
ALLOWED_SYNCHRONOUS_GETS = ("TERMINAL_RECOVERY.get()",)


def java_code_only(text: str) -> str:
    """Remove comments so examples of forbidden APIs do not become audit false positives."""
    return JAVA_LINE_COMMENT.sub("", JAVA_BLOCK_COMMENT.sub("", text))


def blocking_wait_lines(text: str) -> list[str]:
    """Return suspicious reload waits without treating ordinary collection/state-holder gets as Future waits."""
    failures: list[str] = []
    for raw in java_code_only(text).splitlines():
        line = raw.strip()
        if not line:
            continue
        candidate = line
        for allowed in ALLOWED_SYNCHRONOUS_GETS:
            candidate = candidate.replace(allowed, "")
        if BLOCKING_ZERO_ARG_WAIT.search(candidate) or DIRECT_RELOAD_TIMED_GET.search(candidate):
            failures.append(line)
    return failures


def detector_contract_failures() -> list[str]:
    """Self-test the source detector so CI cannot silently reintroduce known false positives or masking."""
    cases = (
        ("cache.get(key);", False, "argument-taking collection get must not be treated as a wait"),
        ("future.get();", True, "zero-argument Future.get must be rejected"),
        ("future.join();", True, "Future.join must be rejected"),
        ("TERMINAL_RECOVERY.get();", False, "known AtomicReference read must remain allowed"),
        (
            "TERMINAL_RECOVERY.get(); future.join();",
            True,
            "allowed AtomicReference read must not mask another blocking wait",
        ),
        ("// future.join();", False, "comment-only blocking examples must be ignored"),
        (
            "client.reloadResourcePacks().get(5, TimeUnit.SECONDS);",
            True,
            "direct timed get on the reload Future must be rejected",
        ),
    )
    failures: list[str] = []
    for sample, expected, label in cases:
        actual = bool(blocking_wait_lines(sample))
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
        if "reloadResourcePacks()" in text:
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
    for line in blocking_wait_lines(texture_text):
        failures.append(f"{texture.relative_to(ROOT)}: blocking reload wait detected: {line}")

    ore = ROOT / next(path for path in RELOAD_CONTROLLERS if "OreHighlightModelReload" in path.name)
    ore_text = ore.read_text(encoding="utf-8")
    for marker in ("AtomicBoolean", "whenComplete", "client.execute", "PENDING"):
        if marker not in java_code_only(ore_text):
            failures.append(f"{ore.relative_to(ROOT)}: missing coalescing reload marker {marker}")
    for line in blocking_wait_lines(ore_text):
        failures.append(f"{ore.relative_to(ROOT)}: blocking reload wait detected: {line}")

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
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
