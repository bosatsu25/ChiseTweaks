#!/usr/bin/env python3
from __future__ import annotations

import unittest

import prism_acceptance_audit as audit


class PrismAcceptanceAuditTest(unittest.TestCase):
    def test_normal_session_with_transport_reset_is_not_a_chise_failure(self) -> None:
        text = "\n".join((
            "Chise diagnostics event=client-startup",
            "Chise diagnostics event=client-join",
            "java.net.SocketException: Connection reset",
            "Chise diagnostics event=client-disconnect",
        ))
        failures, summary = audit.audit(text, require_join=True, require_disconnect=True)
        self.assertEqual([], failures)
        self.assertEqual("transport_disconnect_not_chise_failure", summary["network_reset_classification"])
        self.assertEqual(1, summary["network_reset_count"])
        self.assertEqual(0, summary["retired_runtime_reference_count"])

    def test_component_quarantine_is_a_chise_acceptance_failure(self) -> None:
        text = "\n".join((
            "Chise diagnostics event=client-startup",
            "Chise diagnostics event=component-quarantine",
        ))
        failures, _ = audit.audit(text, require_join=False, require_disconnect=False)
        self.assertTrue(any("component-quarantine" in failure for failure in failures))

    def test_retired_runtime_reference_is_a_failure(self) -> None:
        text = "\n".join((
            "Chise diagnostics event=client-startup",
            "Mixin failed near dev.chise.chisetweaks.mixin.placement.AirPlacementMixin",
        ))
        failures, summary = audit.audit(text, require_join=False, require_disconnect=False)
        self.assertTrue(any("AirPlacementMixin" in failure for failure in failures))
        self.assertEqual(1, summary["retired_runtime_reference_count"])

    def test_ore_model_reload_failure_is_a_failure(self) -> None:
        text = "\n".join((
            "Chise diagnostics event=client-startup",
            "Ore Highlight model reload failed after CompletionException; keeping the current baked models",
        ))
        failures, _ = audit.audit(text, require_join=False, require_disconnect=False)
        self.assertTrue(any("Ore Highlight model reload failed" in failure for failure in failures))

    def test_required_join_and_disconnect_are_enforced(self) -> None:
        failures, _ = audit.audit(
            "Chise diagnostics event=client-startup",
            require_join=True,
            require_disconnect=True,
        )
        self.assertIn("client-join diagnostic event is missing", failures)
        self.assertIn("client-disconnect diagnostic event is missing", failures)


if __name__ == "__main__":
    unittest.main()
