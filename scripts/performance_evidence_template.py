#!/usr/bin/env python3
"""Generate empty, comparable performance evidence sheets for the five required Prism scenarios."""
from __future__ import annotations

import argparse
from pathlib import Path

SCENARIOS = (
    "chise-absent",
    "chise-all-off",
    "analyzers-on",
    "highlights-on",
    "maximum-supported-load",
)
HEADER = (
    "startup_ms,p50_frametime_ms,p95_frametime_ms,p99_frametime_ms,"
    "heap_mib,allocation_mib_s,render_thread_cpu_pct,average_fps"
)


def template(scenario: str, environment_id: str, variant: str) -> str:
    metadata = (
        f"# environment_id={environment_id}\n"
        "# fabric_loader=0.19.3\n"
        "# java=25\n"
        "# minecraft=26.1.2\n"
        f"# scenario={scenario}\n"
        f"# variant={variant}\n"
    )
    return metadata + HEADER + "\n,,,,,,,\n,,,,,,,\n,,,,,,,\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Create ChiseTweaks Prism performance evidence templates.")
    parser.add_argument("output", type=Path)
    parser.add_argument("--environment-id", required=True)
    args = parser.parse_args()

    output = args.output
    output.mkdir(parents=True, exist_ok=True)
    for scenario in SCENARIOS:
        for variant in ("baseline-0.9.3", "candidate-0.9.4"):
            destination = output / f"{scenario}-{variant}.csv"
            if destination.exists():
                raise SystemExit(f"refusing to overwrite existing evidence: {destination.name}")
            destination.write_text(
                template(scenario, args.environment_id.strip(), variant),
                encoding="utf-8",
            )
    print(f"created={len(SCENARIOS) * 2} scenario_templates")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
