package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeDiagnosticReportTest {
    @Test
    void reportUsesVersionedPortableSnapshotWithoutMachineOrServerFields() {
        RuntimeDiagnosticSnapshot snapshot = new RuntimeDiagnosticSnapshot(
                "0.9.4+mc26.1.2",
                42,
                "joined",
                "nether",
                List.of("lavaHighlight", "ancientDebrisAnalyzer"),
                List.of("chisetweaks:chise_chest_visibility"),
                List.of(),
                true,
                false);

        String report = RuntimeDiagnosticReport.format(snapshot);

        assertTrue(report.startsWith("ChiseTweaks Diagnostic Snapshot\nschema=1\n"));
        assertTrue(report.contains("version=0.9.4+mc26.1.2"));
        assertTrue(report.contains("dimension=nether"));
        assertTrue(report.contains("reloadState=reloading"));
        assertFalse(report.contains("server="));
        assertFalse(report.contains("address="));
        assertFalse(report.contains("path="));
    }
}
