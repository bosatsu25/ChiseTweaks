#!/usr/bin/env python3
"""Produce a deterministic size/duplication map for an exact ChiseTweaks runtime JAR."""
from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from collections import defaultdict
from pathlib import Path


def package_for(class_name: str) -> str:
    path = class_name.removesuffix(".class")
    return path.rsplit("/", 1)[0].replace("/", ".") if "/" in path else "<default>"


def build_report(jar: Path, top: int) -> dict[str, object]:
    if not jar.is_file():
        raise FileNotFoundError(jar)

    archive_bytes = jar.stat().st_size
    entries: list[dict[str, object]] = []
    package_bytes: dict[str, int] = defaultdict(int)
    payload_groups: dict[tuple[int, str], list[dict[str, object]]] = defaultdict(list)
    class_count = 0
    synthetic_or_inner_count = 0
    compressed_payload_bytes = 0

    with zipfile.ZipFile(jar) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            compressed_payload_bytes += info.compress_size
            is_class = info.filename.endswith(".class")
            if is_class:
                class_count += 1
                if "$" in Path(info.filename).name:
                    synthetic_or_inner_count += 1
                package_bytes[package_for(info.filename)] += info.compress_size

            payload = archive.read(info)
            digest = hashlib.sha256(payload).hexdigest()
            payload_groups[(len(payload), digest)].append({
                "name": info.filename,
                "compressed_bytes": info.compress_size,
            })
            entries.append({
                "name": info.filename,
                "compressed_bytes": info.compress_size,
                "uncompressed_bytes": info.file_size,
                "kind": "class" if is_class else "resource",
            })

    entries.sort(key=lambda item: (-int(item["compressed_bytes"]), str(item["name"])))
    packages = sorted(
        ({"package": name, "compressed_bytes": size} for name, size in package_bytes.items()),
        key=lambda item: (-int(item["compressed_bytes"]), str(item["package"])),
    )

    duplicates: list[dict[str, object]] = []
    for (size, digest), records in payload_groups.items():
        if len(records) < 2 or size == 0:
            continue
        ordered = sorted(records, key=lambda item: str(item["name"]))
        compressed_sizes = [int(item["compressed_bytes"]) for item in ordered]
        duplicates.append({
            "uncompressed_bytes_each": size,
            "copies": len(ordered),
            "potential_uncompressed_duplicate_bytes": size * (len(ordered) - 1),
            "potential_compressed_duplicate_bytes": sum(compressed_sizes) - min(compressed_sizes),
            "sha256": digest,
            "entries": [str(item["name"]) for item in ordered],
        })
    duplicates.sort(
        key=lambda item: (
            -int(item["potential_compressed_duplicate_bytes"]),
            str(item["entries"][0]),
        )
    )

    return {
        "artifact_name": jar.name,
        "archive_bytes": archive_bytes,
        "compressed_payload_bytes": compressed_payload_bytes,
        "container_overhead_bytes": archive_bytes - compressed_payload_bytes,
        "entry_count": len(entries),
        "class_count": class_count,
        "synthetic_or_inner_class_count": synthetic_or_inner_count,
        "top_entries": entries[:top],
        "top_class_packages": packages[:top],
        "duplicate_payload_groups": duplicates[:top],
        "duplicate_group_count": len(duplicates),
        "potential_compressed_duplicate_bytes": sum(
            int(item["potential_compressed_duplicate_bytes"]) for item in duplicates
        ),
    }


def markdown(report: dict[str, object]) -> str:
    lines = [
        "# Runtime JAR size map",
        "",
        f"- Artifact: `{report['artifact_name']}`",
        f"- Archive: {report['archive_bytes']:,} bytes",
        f"- Compressed entry payloads: {report['compressed_payload_bytes']:,} bytes",
        f"- ZIP/JAR container overhead: {report['container_overhead_bytes']:,} bytes",
        f"- Entries: {report['entry_count']}",
        f"- Classes: {report['class_count']}",
        f"- Inner/synthetic-name classes: {report['synthetic_or_inner_class_count']}",
        f"- Duplicate payload groups: {report['duplicate_group_count']}",
        f"- Potential compressed duplicate payload: {report['potential_compressed_duplicate_bytes']:,} bytes",
        "",
        "## Largest entries",
        "",
        "| Entry | Kind | Compressed | Uncompressed |",
        "| --- | --- | ---: | ---: |",
    ]
    for item in report["top_entries"]:
        lines.append(
            f"| `{item['name']}` | {item['kind']} | {item['compressed_bytes']:,} | {item['uncompressed_bytes']:,} |"
        )

    lines.extend((
        "",
        "## Largest class packages",
        "",
        "| Package | Compressed bytes |",
        "| --- | ---: |",
    ))
    for item in report["top_class_packages"]:
        lines.append(f"| `{item['package']}` | {item['compressed_bytes']:,} |")

    lines.extend(("", "## Duplicate payload groups", ""))
    duplicates = report["duplicate_payload_groups"]
    if not duplicates:
        lines.append("No duplicate non-empty payloads detected.")
    else:
        for item in duplicates:
            lines.append(
                f"- {item['copies']} copies × {item['uncompressed_bytes_each']:,} bytes; "
                f"potential compressed saving {item['potential_compressed_duplicate_bytes']:,} bytes: "
                + ", ".join(f"`{name}`" for name in item["entries"])
            )
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Map compressed runtime JAR size without modifying it.")
    parser.add_argument("jar", type=Path)
    parser.add_argument("--top", type=int, default=20)
    parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    args = parser.parse_args()
    if args.top < 1 or args.top > 200:
        parser.error("--top must be between 1 and 200")

    report = build_report(args.jar, args.top)
    if args.format == "json":
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        print(markdown(report), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
