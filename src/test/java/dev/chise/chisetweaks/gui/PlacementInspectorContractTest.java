package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlacementInspectorContractTest {
    private static final Path TRACKER = Path.of(
            "src/main/java/dev/chise/chisetweaks/gui/PlacementInspector.java");

    @Test
    void resultModelIsExplicitAndBounded() {
        assertEquals(-1, PlacementInspector.NONE);
        assertEquals(0, PlacementInspector.UNAVAILABLE);
        assertEquals(1, PlacementInspector.MATCH);
        assertEquals(2, PlacementInspector.ADJUSTED);
        assertEquals(3, PlacementInspector.DIFFERENT);
        assertEquals(100L, PlacementInspector.EXPIRY_TICKS);
        assertEquals(2L, PlacementInspector.SETTLE_TICKS);
    }

    @Test
    void schematicSnapshotRetainsOnlyUiSafeIdentifiersAndResult() {
        Set<String> components = java.util.Arrays.stream(
                        PlacementInspector.SchematicSnapshot.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("targetPos", "result", "expectedId", "predictedId"), components);
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
        assertTrue(source.contains("refreshSchematic(client)"));
        assertTrue(source.contains("CrosshairInspector.placementProbe("));
        assertTrue(source.contains("SchematicPlacementComparisonPolicy.compare("));
        assertFalse(source.contains("List<"));
        assertFalse(source.contains("Map<"));
        for (String forbidden : new String[]{
                "setBlock(", "setBlockAndUpdate(", ".place(", "sendPacket", "clickMouse",
                "pressMouse", "keyPress", ".clip(", "raycast(", "LOGGER", "config/"}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void unifiedPlacementInspectorUsesOneRuntimeLifecycleWithoutBecomingAFeature() throws Exception {
        String manager = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String session = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java"));

        assertTrue(manager.contains("registerComponent(new PlacementInspector())"));
        assertFalse(manager.contains("registerComponent(new SchematicPlacementInspector())"));
        assertFalse(manager.contains("registerComponent(new PlacementComparisonTracker())"));
        assertTrue(session.contains("FeatureManager.getInstance().resetSessionState(client)"));
    }
}
