package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlacementComparisonTrackerContractTest {
    private static final Path TRACKER = Path.of(
            "src/main/java/dev/chise/chisetweaks/gui/PlacementComparisonTracker.java");

    @Test
    void resultModelIsExplicitAndBounded() {
        assertEquals(-1, PlacementComparisonTracker.NONE);
        assertEquals(0, PlacementComparisonTracker.UNAVAILABLE);
        assertEquals(1, PlacementComparisonTracker.MATCH);
        assertEquals(2, PlacementComparisonTracker.ADJUSTED);
        assertEquals(3, PlacementComparisonTracker.DIFFERENT);
        assertEquals(100L, PlacementComparisonTracker.EXPIRY_TICKS);
        assertEquals(2L, PlacementComparisonTracker.SETTLE_TICKS);
    }

    @Test
    void observerIsSinglePositionMemoryOnlyAndNeverChangesVanillaInput() throws Exception {
        String source = Files.readString(TRACKER);

        assertTrue(source.contains("UseBlockCallback.EVENT.register"));
        assertTrue(source.contains("return InteractionResult.PASS"));
        assertTrue(source.contains("hand != InteractionHand.MAIN_HAND"));
        assertTrue(source.contains("level.isLoaded(targetPos)"));
        assertTrue(source.contains("tick - creationTick > EXPIRY_TICKS"));
        assertTrue(source.contains("pendingLevel != level"));
        assertFalse(source.contains("List<"));
        assertFalse(source.contains("Map<"));
        for (String forbidden : new String[]{
                "setBlock(", "setBlockAndUpdate(", ".place(", "sendPacket", "clickMouse",
                "pressMouse", "keyPress", ".clip(", "raycast(", "LOGGER", "config/"}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void trackerUsesExistingRuntimeLifecycleWithoutBecomingAFeature() throws Exception {
        String manager = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String session = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java"));

        assertTrue(manager.contains("registerComponent(new PlacementComparisonTracker())"));
        assertTrue(session.contains("FeatureManager.getInstance().resetSessionState(client)"));
    }
}
