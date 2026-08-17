#!/usr/bin/env python3
"""Compare ChiseTweaks manual performance captures without adding runtime instrumentation.

Input files are JSON objects containing a scenario name and one or more runs. Each run may contain:
startup_ms, p50_frametime_ms, p95_frametime_ms, p99_frametime_ms, heap_mib,
allocation_mib_s, render_thread_cpu_pct, and average_fps.

The script reports medians and relative deltas. It intentionally does not impose a universal pass/fail
threshold because consumer-PC performance noise must be calibrated from repeated local captures first.
"""
from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path
from typing import Any

METRICS = (
    "startup_ms",
    "p50_frametime_ms",
    "p95_frametime_ms",
    "p99_frametime_ms",
    "heap_mib",
    "allocation_mib_s",
    "render_thread_cpu_pct",
    "average_fps",
)
LOWER_IS_BETTER = set(METRICS) - {"average_fps"}


def load_capture(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    runs = data.get("runs")
    if not isinstance(runs, list) or len(runs) < 3:
        raise ValueError(f"{path}: expected at least three runs")
    return data


def medians(data: dict[str, Any]) -> dict[str, float]:
    result: dict[str, float] = {}
    for metric in METRICS:
        values = [float(run[metric]) for run in data["runs"] if metric in run]
        if values:
            result[metric] = statistics.median(values)
    return result


def percent_delta(baseline: float, candidate: float) -> float | None:
    if baseline == 0.0:
        return None
    return ((candidate - baseline) / baseline) * 100.0


def verdict(metric: str, delta: float | None) -> str:
    if delta is None or abs(delta) < 0.01:
        return "≈"
    improved = delta < 0.0 if metric in LOWER_IS_BETTER else delta > 0.0
    return "better" if improved else "worse"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    args = parser.parse_args()

    baseline = load_capture(args.baseline)
    candidate = load_capture(args.candidate)
    base = medians(baseline)
    cand = medians(candidate)

    print(f"baseline={baseline.get('scenario', args.baseline.name)}")
    print(f"candidate={candidate.get('scenario', args.candidate.name)}")
    print("metric\tbaseline_median\tcandidate_median\tdelta_pct\tdirection")

    common = [metric for metric in METRICS if metric in base and metric in cand]
    if not common:
        raise ValueError("captures share no supported metrics")

    for metric in common:
        delta = percent_delta(base[metric], cand[metric])
        delta_text = "n/a" if delta is None else f"{delta:+.3f}"
        print(
            f"{metric}\t{base[metric]:.3f}\t{cand[metric]:.3f}\t"
            f"{delta_text}\t{verdict(metric, delta)}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
