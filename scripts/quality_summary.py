#!/usr/bin/env python3
"""Create a compact quality summary from JUnit, JaCoCo and PIT reports."""
from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from collections.abc import Callable
from pathlib import Path
from typing import TypeVar

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build"
CI_DIR = BUILD / "ci"
SUMMARY = CI_DIR / "quality-summary.md"
T = TypeVar("T")


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def junit_totals() -> tuple[int, int, int, int]:
    files = sorted((BUILD / "test-results/test").glob("TEST-*.xml"))
    if not files:
        raise RuntimeError("JUnit XML reports are missing")
    tests = failures = errors = skipped = 0
    for path in files:
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get("tests", 0))
        failures += int(root.attrib.get("failures", 0))
        errors += int(root.attrib.get("errors", 0))
        skipped += int(root.attrib.get("skipped", 0))
    return tests, failures, errors, skipped


def jacoco_lines() -> tuple[int, int]:
    path = BUILD / "reports/jacoco/test/jacocoTestReport.xml"
    if not path.is_file():
        raise RuntimeError("JaCoCo XML report is missing")
    root = ET.parse(path).getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") == "LINE":
            missed = int(counter.attrib["missed"])
            covered = int(counter.attrib["covered"])
            return missed, covered
    raise RuntimeError("JaCoCo LINE counter is missing")


def pit_totals() -> tuple[int, int, int, int]:
    path = BUILD / "reports/pitest/mutations.xml"
    if not path.is_file():
        raise RuntimeError("PIT mutations.xml report is missing")
    root = ET.parse(path).getroot()
    mutations = list(root.findall("mutation"))
    if not mutations:
        raise RuntimeError("PIT report contains no mutations")
    killed = sum(1 for mutation in mutations if mutation.attrib.get("status") == "KILLED")
    survived = sum(1 for mutation in mutations if mutation.attrib.get("status") == "SURVIVED")
    no_coverage = sum(1 for mutation in mutations if mutation.attrib.get("status") == "NO_COVERAGE")
    return len(mutations), killed, survived, no_coverage


def percent(numerator: int, denominator: int) -> float:
    return 100.0 if denominator == 0 else numerator * 100.0 / denominator


def optional_metric(name: str, reader: Callable[[], T], allow_partial: bool) -> tuple[T | None, str | None]:
    try:
        return reader(), None
    except (OSError, ET.ParseError, RuntimeError, ValueError, KeyError) as error:
        if not allow_partial:
            raise RuntimeError(f"{name}: {error}") from error
        return None, str(error)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--allow-partial",
        action="store_true",
        help="Write a diagnostic summary even when an upstream gate did not produce every report.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        properties = read_properties(ROOT / "gradle.properties")
        junit, junit_error = optional_metric("JUnit", junit_totals, args.allow_partial)
        jacoco, jacoco_error = optional_metric("JaCoCo", jacoco_lines, args.allow_partial)
        pit, pit_error = optional_metric("PIT", pit_totals, args.allow_partial)
    except (OSError, RuntimeError, ValueError, KeyError) as error:
        print(f"QUALITY SUMMARY: FAIL: {error}", file=sys.stderr)
        return 1

    line_threshold = float(properties.get("quality_line_threshold", "0.96")) * 100.0
    mutation_threshold = float(properties.get("quality_mutation_threshold", "96"))
    strength_threshold = float(properties.get("quality_test_strength_threshold", "96"))

    lines = [
        "## ChiseTweaks retained-scope quality summary",
        "",
    ]

    if junit is None:
        lines.append(f"- JUnit: **unavailable** — {junit_error}")
    else:
        tests, failures, errors, skipped = junit
        lines.append(
            f"- JUnit: **{tests}** tests, **{failures}** failures, "
            f"**{errors}** errors, **{skipped}** skipped"
        )

    if jacoco is None:
        lines.append(f"- JaCoCo line coverage: **unavailable** — {jacoco_error}")
    else:
        line_missed, line_covered = jacoco
        line_total = line_missed + line_covered
        line_coverage = percent(line_covered, line_total)
        lines.append(
            f"- JaCoCo line coverage: **{line_covered}/{line_total} = {line_coverage:.2f}%** "
            f"(gate {line_threshold:.2f}%)"
        )

    if pit is None:
        lines.append(f"- PIT mutation score: **unavailable** — {pit_error}")
        lines.append("- PIT test strength: **unavailable**")
    else:
        mutations, killed, survived, no_coverage = pit
        mutation_score = percent(killed, mutations)
        test_strength_denominator = killed + survived
        test_strength = percent(killed, test_strength_denominator)
        lines.extend([
            f"- PIT mutation score: **{killed}/{mutations} = {mutation_score:.2f}%** "
            f"(gate {mutation_threshold:.2f}%)",
            f"- PIT test strength: **{killed}/{test_strength_denominator} = {test_strength:.2f}%** "
            f"(gate {strength_threshold:.2f}%)",
            f"- PIT survived: **{survived}**; no coverage: **{no_coverage}**",
        ])

    if args.allow_partial and any(error is not None for error in (junit_error, jacoco_error, pit_error)):
        lines.extend([
            "",
            "> Partial diagnostic mode: unavailable reports indicate an upstream gate did not complete.",
            "> The final GitHub Actions aggregation step still fails the workflow when a required gate fails.",
        ])

    lines.extend([
        "",
        "The percentage gate intentionally measures the deterministic policy kernel of the retained features.",
        "Renderer/mixin behavior is covered by repository contracts plus manual Prism gameplay smoke tests.",
        "",
    ])

    CI_DIR.mkdir(parents=True, exist_ok=True)
    SUMMARY.write_text("\n".join(lines), encoding="utf-8")
    print("QUALITY SUMMARY: PASS" if not args.allow_partial else "QUALITY SUMMARY: DIAGNOSTIC")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
