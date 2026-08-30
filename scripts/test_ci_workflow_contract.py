#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"
RETIRED_RELEASE_WORKFLOW = ROOT / ".github/workflows/release.yml"


def require(text: str, marker: str, label: str) -> None:
    if marker not in text:
        raise AssertionError(f"{label}: missing {marker!r}")


def main() -> int:
    ci = CI_WORKFLOW.read_text(encoding="utf-8")
    trigger_block = ci[ci.index("on:"):ci.index("\npermissions:")]

    require(trigger_block, "pull_request:", "automatic pull request trigger")
    require(trigger_block, "- opened", "pull request opened trigger")
    require(trigger_block, "- synchronize", "pull request synchronize trigger")
    require(trigger_block, "- reopened", "pull request reopened trigger")
    require(trigger_block, "- ready_for_review", "draft-to-ready trigger")
    require(trigger_block, "branches:", "pull request branch filter")
    require(trigger_block, "- main", "main branch pull request target")
    require(trigger_block, "workflow_dispatch:", "manual CI fallback")
    if "  push:" in trigger_block:
        raise AssertionError("automatic main push/release must remain paused during runner stabilization")

    require(ci, "name: verify / Java 25 quality gate", "required status-check name")
    require(ci, "actions: read", "prior-run artifact permission")
    require(ci, "- name: Resolve prior PR verification", "main provenance resolver")
    require(ci, "commits/${HEAD_SHA}/pulls", "merged PR lookup")
    require(ci, "event=pull_request&status=success", "successful PR CI lookup")
    require(ci, "name: chise-ci-provenance", "provenance artifact")
    require(ci, "python3 scripts/ci_scope.py --file-list", "scope classifier")
    require(ci, "python3 scripts/ci_provenance.py decide", "tree provenance decision")
    require(ci, "--diff-filter=ACDMRTUXB", "deletion-aware diff")
    require(ci, "reason=direct-or-unverified-main", "fail-closed main fallback")
    require(ci, "- name: Download tree-identical verified runtime", "runtime promotion download")
    require(ci, "python scripts/promoted_artifact_audit.py", "promoted artifact audit")
    require(ci, "python -m compileall -q scripts", "Python tooling compile check")
    require(ci, "python scripts/test_ci_provenance.py", "provenance self-test")
    require(ci, "- name: Write reusable PR provenance", "provenance writer")
    require(ci, "tested_tree", "tested tree capture")
    require(ci, "steps.runtime.outputs.runtime_jar != ''", "runtime upload gate")

    require(ci, "github.event.pull_request.draft == false", "draft PR skip")
    require(ci, "timeout-minutes: 15", "CI runaway budget cap")
    require(ci, "retention-days: 3", "short-lived CI artifact")
    require(ci, "vars.CHISE_CI_RUNS_ON", "runner override")

    require(ci, "name: release / Publish verified runtime JAR", "integrated release job")
    require(ci, "needs.verify.outputs.release_ready == 'true'", "runtime-only release gate")
    require(ci, "contents: write", "release write permission")
    require(ci, "timeout-minutes: 5", "release runaway budget cap")
    require(ci, "name: ${{ needs.verify.outputs.runtime_jar }}", "same-run exact artifact download")
    require(ci, "Exact CI artifact promoted", "release artifact integrity summary")

    if RETIRED_RELEASE_WORKFLOW.exists():
        raise AssertionError("standalone release workflow must remain retired")

    heavy_condition = "if: ${{ steps.scope.outputs.heavy == 'true' }}"
    if ci.count(heavy_condition) < 6:
        raise AssertionError("heavy runtime steps are no longer consistently scope-gated")

    audit_index = ci.index("- name: Audit repository contracts")
    promotion_index = ci.index("- name: Download tree-identical verified runtime")
    java_index = ci.index("- name: Set up Java 25")
    if not (audit_index < promotion_index < java_index):
        raise AssertionError("static audits must fail fast before promotion or Java/Gradle execution")

    release_index = ci.index("  release:")
    release_candidate_index = ci.index("- name: Resolve release candidate")
    if release_candidate_index >= release_index:
        raise AssertionError("verify job must decide runtime release readiness before release job")

    print("CI WORKFLOW CONTRACT TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
