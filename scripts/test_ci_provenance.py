#!/usr/bin/env python3
from ci_provenance import promotion_decision


BASE = {
    "schema": 1,
    "repository": "owner/repo",
    "source_run_id": 42,
    "event": "pull_request",
    "scope": "full",
    "tested_commit_sha": "a" * 40,
    "tested_tree_sha": "b" * 40,
    "version": "0.15.0+mc26.1.2",
    "runtime_verified": True,
    "runtime_jar": "chise-tweaks-0.15.0+mc26.1.2.jar",
    "runtime_sha256": "c" * 64,
}


def decide(payload, *, tree="b" * 40, run_id=42, has_runtime=True):
    return promotion_decision(
        payload,
        repository="owner/repo",
        expected_run_id=run_id,
        current_tree_sha=tree,
        has_runtime_artifact=has_runtime,
    )


def main() -> int:
    result = decide(dict(BASE))
    assert result == {
        "scope": "full", "heavy": False, "promote": True, "reason": "tree-identical-full"}

    assert decide(dict(BASE), tree="d" * 40)["heavy"] is True
    assert decide(dict(BASE), has_runtime=False)["heavy"] is True
    assert decide(dict(BASE), run_id=99)["heavy"] is True

    docs = dict(BASE)
    docs.update(scope="docs-only", runtime_verified=False, runtime_jar="", runtime_sha256="")
    assert decide(docs) == {
        "scope": "docs-only", "heavy": False, "promote": False,
        "reason": "tree-identical-non-runtime"}

    tooling = dict(docs)
    tooling["scope"] = "tooling-only"
    assert decide(tooling)["heavy"] is False

    malformed = dict(BASE)
    malformed["repository"] = "other/repo"
    assert decide(malformed)["heavy"] is True

    print("CI PROVENANCE TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
