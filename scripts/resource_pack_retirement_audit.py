#!/usr/bin/env python3
"""Read-only preflight for ChiseTweaks external Resource Pack retirement acceptance."""
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True)
class Candidate:
    key: str
    display_name: str
    present: bool
    matched_files: tuple[str, ...]
    decision: str
    required_evidence: str


def normalized(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", name.casefold())


def classify(filename: str) -> set[str]:
    value = normalized(filename)
    matches: set[str] = set()
    if "amateras" in value:
        matches.add("amateras")
    if "newglowingores" in value or ("glowingores" in value and "new" in value):
        matches.add("new_glowing_ores")
    if "lowonfire" in value:
        matches.add("low_on_fire")
    if "chise" in value and ("texture" in value or "resource" in value or "pack" in value):
        matches.add("legacy_chise")
    return matches


def inventory(instance_root: Path) -> dict[str, object]:
    resourcepacks = instance_root / "resourcepacks"
    names: list[str] = []
    if resourcepacks.is_dir():
        names = sorted(
            entry.name
            for entry in resourcepacks.iterdir()
            if not entry.name.startswith(".")
        )

    buckets: dict[str, list[str]] = {
        "amateras": [],
        "new_glowing_ores": [],
        "low_on_fire": [],
        "legacy_chise": [],
    }
    for name in names:
        for key in classify(name):
            buckets[key].append(name)

    specs = (
        ("amateras", "AMATERAS Resource Pack / RPO", "Functional workflow parity in the supported BuilderPack plus rollback evidence."),
        ("new_glowing_ores", "NewGlowingOres", "Ore Highlights visual acceptance with vanilla, Sodium, Iris-disabled and supported shader-enabled configurations."),
        ("low_on_fire", "LowOnFire external pack", "Low Fire visual acceptance and immediate vanilla restoration when disabled."),
        ("legacy_chise", "Legacy external Chise texture/resource pack", "Confirm the retained 12-feature Chise runtime replaces the specific pack workflow without unrelated visual loss."),
    )
    candidates = [
        Candidate(key, display, bool(buckets[key]), tuple(buckets[key]), "REVIEW" if buckets[key] else "ABSENT", evidence)
        for key, display, evidence in specs
    ]
    rpo_files = tuple(name for name in names if name.casefold().endswith(".rpo"))
    return {
        "resourcepacks_directory_present": resourcepacks.is_dir(),
        "mutation_performed": False,
        "candidate_count_present": sum(candidate.present for candidate in candidates),
        "candidates": [asdict(candidate) for candidate in candidates],
        "rpo_files": rpo_files,
        "respackopts_retirement_preflight": "NO_RPO_FILES_PRESENT" if not rpo_files else "RPO_DEPENDENCIES_REQUIRE_REVIEW",
        "shader_acceptance_required_for_new_glowing_ores": True,
        "physical_prism_acceptance_required": True,
    }


def markdown(report: dict[str, object]) -> str:
    lines = [
        "# Resource Pack retirement preflight", "",
        "This report is read-only. It does not remove, rename, edit, enable, disable, or reorder Resource Packs.", "",
        "| Candidate | Presence | Decision | Matched files | Required evidence |",
        "| --- | --- | --- | --- | --- |",
    ]
    for raw in report["candidates"]:
        candidate = dict(raw)
        files = ", ".join(f"`{name}`" for name in candidate["matched_files"]) or "—"
        lines.append(f"| {candidate['display_name']} | {'PRESENT' if candidate['present'] else 'ABSENT'} | {candidate['decision']} | {files} | {candidate['required_evidence']} |")
    lines.extend((
        "", f"- `.rpo` files: {len(report['rpo_files'])}",
        f"- RespackOpts preflight: `{report['respackopts_retirement_preflight']}`",
        "- NewGlowingOres retirement always requires shader-sensitive visual acceptance when the pack is present.",
        "- Final RETIRE/KEEP decisions require the real supported Prism/Windows/GPU environment.",
    ))
    if report["rpo_files"]:
        lines.append("- RPO inventory: " + ", ".join(f"`{name}`" for name in report["rpo_files"]))
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Inventory Resource Pack retirement candidates without mutating the Prism instance.")
    parser.add_argument("instance_root", type=Path, help="Prism instance root containing resourcepacks/")
    parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    args = parser.parse_args()
    report = inventory(args.instance_root.expanduser())
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) if args.format == "json" else markdown(report), end="\n" if args.format == "json" else "")
    if not report["resourcepacks_directory_present"]:
        print("resourcepacks directory was not found; preflight is incomplete", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
