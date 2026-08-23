package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertEquals(
                "ChiseTweaks Diagnostic Snapshot\n"
                        + "schema=1\n"
                        + "version=0.9.4+mc26.1.2 sessionId=42 phase=joined dimension=nether "
                        + "enabled=[lavaHighlight,ancientDebrisAnalyzer] "
                        + "visibilityPacks=[chisetweaks:chise_chest_visibility] "
                        + "quarantined=[] reloadState=reloading\n",
                report);
        assertFalse(report.contains("server="));
        assertFalse(report.contains("address="));
        assertFalse(report.contains("path="));
        assertTrue(report.endsWith("\n"));
    }

    @Test
    void missingSnapshotIsRejected() {
        assertThrows(NullPointerException.class, () -> RuntimeDiagnosticReport.format(null));
    }
}
