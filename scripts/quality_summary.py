#!/usr/bin/env python3
"""Create a compact GitHub/CI quality summary from JUnit, JaCoCo and PIT reports."""
from __future__ import annotations

import argparse
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def pct(numerator: int, denominator: int) -> float:
    return 100.0 if denominator == 0 else (100.0 * numerator / denominator)


def junit_totals(directory: Path) -> tuple[int, int, int, int]:
    tests = failures = errors = skipped = 0
    files = list(directory.glob("TEST-*.xml"))
    if not files:
        raise FileNotFoundError(f"no JUnit XML files under {directory}")
    for path in files:
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get("tests", "0"))
        failures += int(root.attrib.get("failures", "0"))
        errors += int(root.attrib.get("errors", "0"))
        skipped += int(root.attrib.get("skipped", "0"))
    return tests, failures, errors, skipped


def jacoco_line(report: Path) -> tuple[int, int, float]:
    root = ET.parse(report).getroot()
    counters = [x for x in root.findall("counter") if x.attrib.get("type") == "LINE"]
    if len(counters) != 1:
        raise ValueError("JaCoCo report has no unique root LINE counter")
    missed = int(counters[0].attrib["missed"])
    covered = int(counters[0].attrib["covered"])
    return covered, missed, pct(covered, covered + missed)


def pit_scores(report: Path) -> tuple[int, int, int, float, float]:
    root = ET.parse(report).getroot()
    mutations = list(root.findall("mutation"))
    if not mutations:
        raise ValueError("PIT report contains no mutations")
    total = len(mutations)
    killed = sum(1 for m in mutations if m.attrib.get("status") == "KILLED")
    no_coverage = sum(1 for m in mutations if m.attrib.get("status") == "NO_COVERAGE")
    tested = total - no_coverage
    return killed, total, tested, pct(killed, total), pct(killed, tested)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ROOT / "build/ci/quality-summary.md")
    args = parser.parse_args()

    try:
        tests, failures, errors, skipped = junit_totals(ROOT / "build/test-results/test")
        covered, missed, line_pct = jacoco_line(ROOT / "build/reports/jacoco/test/jacocoTestReport.xml")
        killed, mutations, tested, mutation_pct, strength_pct = pit_scores(ROOT / "build/reports/pitest/mutations.xml")
    except (OSError, ValueError, ET.ParseError) as exc:
        print(f"QUALITY SUMMARY: FAIL - {exc}", file=sys.stderr)
        sys.exit(1)

    status = "PASS" if failures == 0 and errors == 0 and line_pct >= 96.0 and mutation_pct >= 96.0 and strength_pct >= 96.0 else "FAIL"
    text = f"""## ChiseTweaks Quality Gate

| Gate | Result |
|---|---:|
| JUnit | {tests - failures - errors - skipped}/{tests} passed ({skipped} skipped) |
| Line coverage | {covered}/{covered + missed} = **{line_pct:.2f}%** |
| Mutation score | {killed}/{mutations} = **{mutation_pct:.2f}%** |
| Test strength | {killed}/{tested} = **{strength_pct:.2f}%** |
| Overall | **{status}** |

Thresholds: Line coverage >= 96%, Mutation score >= 96%, Test strength >= 96%.
"""
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(text, encoding="utf-8")
    github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if github_summary:
        with open(github_summary, "a", encoding="utf-8") as handle:
            handle.write(text + "\n")
    print(text)
    if status != "PASS":
        sys.exit(1)


if __name__ == "__main__":
    main()
