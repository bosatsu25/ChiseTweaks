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
BLOCKING_FUTURE_WAIT = re.compile(r"\.(?:join|get)\s*\(")
PLAIN_GET_CHUNK = re.compile(r"\bgetChunk\s*\(")


def blocking_wait_lines(text: str) -> list[str]:
    """Return suspicious blocking waits while excluding known synchronous state-holder reads."""
    failures: list[str] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not BLOCKING_FUTURE_WAIT.search(line):
            continue
        if "TERMINAL_RECOVERY.get()" in line:
            continue
        failures.append(line)
    return failures


def audit() -> list[str]:
    failures: list[str] = []
    reload_callers: set[Path] = set()

    for path in PRODUCTION.rglob("*.java"):
        relative = path.relative_to(ROOT)
        text = path.read_text(encoding="utf-8")
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
        if marker not in texture_text:
            failures.append(f"{texture.relative_to(ROOT)}: missing non-blocking reload marker {marker}")
    for line in blocking_wait_lines(texture_text):
        failures.append(f"{texture.relative_to(ROOT)}: blocking reload wait detected: {line}")

    ore = ROOT / next(path for path in RELOAD_CONTROLLERS if "OreHighlightModelReload" in path.name)
    ore_text = ore.read_text(encoding="utf-8")
    for marker in ("AtomicBoolean", "whenComplete", "client.execute", "PENDING"):
        if marker not in ore_text:
            failures.append(f"{ore.relative_to(ROOT)}: missing coalescing reload marker {marker}")
    for line in blocking_wait_lines(ore_text):
        failures.append(f"{ore.relative_to(ROOT)}: blocking reload wait detected: {line}")

    for relative in ANALYZERS:
        path = ROOT / relative
        text = path.read_text(encoding="utf-8")
        if "getChunkNow(" not in text:
            failures.append(f"{relative}: analyzer must inspect only already-loaded chunks")
        if PLAIN_GET_CHUNK.search(text):
            failures.append(f"{relative}: potentially force-loading getChunk call detected")

    policy = ROOT / "src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java"
    policy_text = policy.read_text(encoding="utf-8")
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
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
