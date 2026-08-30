package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PatternConsistencyInspectorContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/dev/chise/chisetweaks/gui/PatternConsistencyInspector.java");

    @Test
    void scanAndRetentionBudgetsAreHardAndConservative() {
        assertEquals(8, PatternConsistencyInspector.HORIZONTAL_RADIUS);
        assertEquals(4, PatternConsistencyInspector.VERTICAL_RADIUS);
        assertEquals(256, PatternConsistencyInspector.MAX_BLOCKS_PER_TICK);
        assertEquals(64, PatternConsistencyInspector.MAX_RETAINED_MISMATCHES);
        assertEquals(20, PatternConsistencyInspector.RESCAN_INTERVAL_TICKS);
        assertEquals(2_601, PatternConsistencyInspector.TOTAL_BLOCKS);
    }

    @Test
    void scannerUsesOnlyLoadedChunksAndTheExplicitReferenceState() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("selectReference(Minecraft client)"));
        assertTrue(source.contains("candidate.getBlock() != referenceState.getBlock()"));
        assertTrue(source.contains("level.getChunkSource().hasChunk"));
        assertTrue(source.contains("processed++ < MAX_BLOCKS_PER_TICK"));
        assertTrue(source.contains("retainedMismatchCount < retainedMismatchPositions.length"));
        assertFalse(source.contains("getChunk("));
        assertFalse(source.contains("majority"));
        assertFalse(source.contains("frequency"));
        assertFalse(source.contains("nearest"));
    }

    @Test
    void patternInspectionIsReadOnlyMemoryOnlyAndSessionBound() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("implements TickingRuntimeComponent, SessionAwareRuntimeComponent"));
        assertTrue(source.contains("if (level == null || level != referenceLevel)"));
        assertTrue(source.contains("resetSession(Minecraft client)"));
        for (String forbidden : new String[]{
                "setBlock(", "setBlockAndUpdate(", "sendPacket", "UseBlockCallback",
                ".clip(", "raycast(", "CompletableFuture", "Executor", "LOGGER",
                "saveToFile(", "getBlockEntity(", "getNbt(", "getUUID("}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void patternToolReusesInspectorUiWithoutBecomingATopLevelFeature() throws Exception {
        String manager = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String controller = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java"));
        String definitions = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java"));
        String pattern = Files.readString(SOURCE);

        assertTrue(manager.contains("registerComponent(new PatternConsistencyInspector())"));
        assertTrue(controller.contains("addPatternConsistencyRows"));
        assertTrue(pattern.contains("semanticPropertyGroup(property)"));
        assertFalse(definitions.contains("PATTERN_CONSISTENCY"));
        assertEquals(6, ChiseTweaksSettingsController.Surface.values().length);
    }
}
