package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HistoricalConfigFixtureTest {
    @Test
    void version077MigratesLegacyTargetsAndCopiesWorksiteBudgetsToLava() throws IOException {
        LocalFeatureConfig config = load("0.7.7-local.json");
        assertEquals(8, config.lavaAnalyzerHorizontalRadius);
        assertEquals(4, config.lavaAnalyzerVerticalRadius);
        assertEquals(25, config.lavaAnalyzerIntervalTicks);
        assertEquals(20, config.lavaAnalyzerMaxOverlayResults);
        assertEquals(
                VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                        | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                config.visualTargetMask);
    }

    @Test
    void version091AddsTechnicalTargetsWithoutReEnablingOtherExplicitTargets() throws IOException {
        LocalFeatureConfig config = load("0.9.1-local.json");
        int diamond = VisualTargetSelectionPolicy.Target.MATERIAL_DIAMOND_ORE.bitMask();
        assertEquals(diamond | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                config.visualTargetMask);
        assertEquals(6, config.lavaAnalyzerHorizontalRadius);
        assertEquals(20, config.lavaAnalyzerIntervalTicks);
    }

    @Test
    void version092PreservesExplicitAnalyzerValues() throws IOException {
        LocalFeatureConfig config = load("0.9.2-local.json");
        assertTrue(config.lavaHighlightEnabled);
        assertTrue(config.ancientDebrisAnalyzerEnabled);
        assertTrue(config.fireVisibilityEnabled);
        assertEquals(7, config.lavaAnalyzerHorizontalRadius);
        assertEquals(2, config.lavaAnalyzerVerticalRadius);
        assertEquals(30, config.lavaAnalyzerIntervalTicks);
        assertEquals(21, config.lavaAnalyzerMaxOverlayResults);
        assertEquals(192, config.ancientDebrisAnalyzerRangeBlocks);
        assertEquals(96, config.ancientDebrisAnalyzerMaxMarkers);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
    }

    @Test
    void futureFixtureKeepsKnownBitsAndDropsUnknownFieldsAndBits() throws IOException {
        LocalFeatureConfig config = load("future-local.json");
        assertEquals(VisualTargetSelectionPolicy.Target.MATERIAL_DIAMOND_ORE.bitMask(),
                config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION,
                config.visualTargetSchemaVersion);
    }

    private static LocalFeatureConfig load(String fixture) throws IOException {
        String resource = "/config-fixtures/" + fixture;
        try (InputStream stream = HistoricalConfigFixtureTest.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing fixture: " + resource);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LocalFeatureConfig config = new LocalFeatureConfig();
            assertTrue(config.replaceFromJsonDocument(json), fixture);
            return config;
        }
    }
}
