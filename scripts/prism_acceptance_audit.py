#!/usr/bin/env python3
"""Classify a Prism latest.log without treating ordinary network resets as Chise crashes."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

DIAGNOSTIC_EVENT = re.compile(r"Chise diagnostics event=([a-z0-9-]+)")
CRITICAL_MIXIN_MARKERS = (
    "MixinTransformerError",
    "Mixin apply failed",
    "MixinApplyError",
    "InvalidMixinException",
)
CHISE_FAILURE_EVENTS = {
    "component-init-quarantine",
    "component-quarantine",
    "resource-pack-selection-failure",
    "resource-pack-rollback-failure",
    "resource-reload-failure",
    "resource-reload-terminal-failure",
}
NETWORK_RESET_MARKERS = (
    "java.net.SocketException: Connection reset",
    "Connection reset",
)


def audit(text: str, require_join: bool, require_disconnect: bool) -> tuple[list[str], dict[str, object]]:
    failures: list[str] = []
    events = DIAGNOSTIC_EVENT.findall(text)
    event_set = set(events)

    for marker in CRITICAL_MIXIN_MARKERS:
        if marker in text:
            failures.append(f"critical mixin marker found: {marker}")

    for event in sorted(event_set & CHISE_FAILURE_EVENTS):
        failures.append(f"Chise failure diagnostic event found: {event}")

    if "client-startup" not in event_set:
        failures.append("client-startup diagnostic event is missing")
    if require_join and "client-join" not in event_set:
        failures.append("client-join diagnostic event is missing")
    if require_disconnect and "client-disconnect" not in event_set:
        failures.append("client-disconnect diagnostic event is missing")

    snapshots = [line.strip() for line in text.splitlines() if "Chise diagnostics event=" in line]
    network_resets = sum(text.count(marker) for marker in NETWORK_RESET_MARKERS[:1])
    summary: dict[str, object] = {
        "events": events,
        "snapshot_count": len(snapshots),
        "network_reset_count": network_resets,
        "network_reset_classification": "transport_disconnect_not_chise_failure",
    }
    return failures, summary


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit a Prism/Minecraft latest.log for Chise acceptance evidence.")
    parser.add_argument("log", type=Path)
    parser.add_argument("--require-join", action="store_true")
    parser.add_argument("--require-disconnect", action="store_true")
    args = parser.parse_args()

    try:
        text = args.log.read_text(encoding="utf-8", errors="replace")
    except OSError as error:
        print(f"PRISM ACCEPTANCE AUDIT: ERROR: {error}", file=sys.stderr)
        return 2

    failures, summary = audit(text, args.require_join, args.require_disconnect)
    print(f"diagnostic_events={','.join(summary['events']) or 'none'}")
    print(f"diagnostic_snapshots={summary['snapshot_count']}")
    print(f"network_resets={summary['network_reset_count']}")
    print(f"network_reset_classification={summary['network_reset_classification']}")

    if failures:
        print("PRISM ACCEPTANCE AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("PRISM ACCEPTANCE AUDIT: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
