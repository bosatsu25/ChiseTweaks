package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RuntimeDiagnosticSnapshotTest {
    @Test
    void rendersStableSingleLineDiagnosticRecord() {
        RuntimeDiagnosticSnapshot snapshot = new RuntimeDiagnosticSnapshot(
                "0.9.3+mc26.1.2",
                7L,
                "joined",
                "nether",
                List.of("lava_highlight", "ancient_debris_analyzer"),
                List.of("chisetweaks:chise_chest_visibility"),
                List.of("worksite_visibility"),
                true,
                false);

        assertEquals(
                "version=0.9.3+mc26.1.2 sessionId=7 phase=joined dimension=nether "
                        + "enabled=[lava_highlight,ancient_debris_analyzer] "
                        + "visibilityPacks=[chisetweaks:chise_chest_visibility] "
                        + "quarantined=[worksite_visibility] reloadState=reloading",
                snapshot.toLogLine());
    }

    @Test
    void reloadStateCoversEveryCoordinatorStateCombination() {
        assertEquals("idle", snapshot(false, false).reloadState());
        assertEquals("reloading", snapshot(true, false).reloadState());
        assertEquals("recovery_pending", snapshot(false, true).reloadState());
        assertEquals("reloading_with_recovery", snapshot(true, true).reloadState());
    }

    @Test
    void defensivelyCopiesDiagnosticLists() {
        ArrayList<String> enabled = new ArrayList<>(List.of("lava_highlight"));
        RuntimeDiagnosticSnapshot snapshot = new RuntimeDiagnosticSnapshot(
                "0.9.3+mc26.1.2", 1L, "joined", "overworld",
                enabled, List.of(), List.of(), false, false);
        enabled.add("ancient_debris_analyzer");
        assertEquals(List.of("lava_highlight"), snapshot.enabledFeatures());
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeDiagnosticSnapshot(
                " ", 1L, "joined", "overworld", List.of(), List.of(), List.of(), false, false));
    }

    private static RuntimeDiagnosticSnapshot snapshot(boolean reloading, boolean recovery) {
        return new RuntimeDiagnosticSnapshot(
                "0.9.3+mc26.1.2",
                1L,
                "joined",
                "overworld",
                List.of(),
                List.of(),
                List.of(),
                reloading,
                recovery);
    }
}
