#!/usr/bin/env python3
import json
from pathlib import Path
from tempfile import TemporaryDirectory

from ci_provenance import promotion_decision, write_provenance


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

    assert decide(dict(BASE), tree="d" * 40)["reason"] == "tree-mismatch"
    assert decide(dict(BASE), tree="NOT-A-SHA")["reason"] == "tree-sha-format"
    assert decide(dict(BASE), has_runtime=False)["reason"] == "runtime-artifact-missing"
    assert decide(dict(BASE), run_id=99)["reason"] == "run-id"

    docs = dict(BASE)
    docs.update(scope="docs-only", runtime_verified=False, runtime_jar="", runtime_sha256="")
    assert decide(docs) == {
        "scope": "docs-only", "heavy": False, "promote": False,
        "reason": "tree-identical-non-runtime"}

    tooling = dict(docs)
    tooling["scope"] = "tooling-only"
    assert decide(tooling)["heavy"] is False

    forged_docs = dict(docs)
    forged_docs["runtime_jar"] = "chise-tweaks-forged.jar"
    assert decide(forged_docs)["reason"] == "non-runtime-claimed-artifact"

    malformed = dict(BASE)
    malformed["repository"] = "other/repo"
    assert decide(malformed)["reason"] == "repository"

    malformed_hash = dict(BASE)
    malformed_hash["runtime_sha256"] = "Z" * 64
    assert decide(malformed_hash)["reason"] == "runtime-sha256"

    with TemporaryDirectory() as directory:
        output = Path(directory) / "ci-provenance.json"
        write_provenance(
            output,
            repository="owner/repo",
            run_id=42,
            event="pull_request",
            scope="full",
            tested_commit_sha="a" * 40,
            tested_tree_sha="b" * 40,
            version="0.15.0+mc26.1.2",
            runtime_jar="chise-tweaks-0.15.0+mc26.1.2.jar",
            runtime_sha256="c" * 64,
            runtime_verified=True,
        )
        written = json.loads(output.read_text(encoding="utf-8"))
        assert written["tested_tree_sha"] == "b" * 40
        assert written["runtime_verified"] is True

    try:
        with TemporaryDirectory() as directory:
            write_provenance(
                Path(directory) / "invalid.json",
                repository="owner/repo",
                run_id=42,
                event="pull_request",
                scope="full",
                tested_commit_sha="a" * 40,
                tested_tree_sha="not-a-tree",
                version="0.15.0+mc26.1.2",
                runtime_jar="chise-tweaks-0.15.0+mc26.1.2.jar",
                runtime_sha256="c" * 64,
                runtime_verified=True,
            )
        raise AssertionError("invalid tree SHA must be rejected")
    except ValueError:
        pass

    print("CI PROVENANCE TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
