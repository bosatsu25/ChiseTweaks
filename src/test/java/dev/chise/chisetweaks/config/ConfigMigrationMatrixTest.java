package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigMigrationMatrixTest {
    private static final Path FIXTURES = Path.of("src/test/resources/config-fixtures");

    @Test
    void everyRetainedHistoricalAndFutureFixtureMigratesToCurrentSchema() throws Exception {
        for (String fixture : new String[]{
                "0.7.7-local.json",
                "0.9.1-local.json",
                "0.9.2-local.json",
                "future-local.json"}) {
            LocalFeatureConfig config = load(fixture);
            assertEquals(
                    VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION,
                    config.visualTargetSchemaVersion,
                    fixture);
        }
    }

    @Test
    void schemaOneFixtureAddsBothLaterTargetFamiliesAndKeepsBoundedValues() throws Exception {
        LocalFeatureConfig config = load("0.7.7-local.json");

        assertFalse(config.lavaHighlightEnabled);
        assertEquals(8, config.worksiteVisibilityHorizontalRadius);
        assertEquals(4, config.worksiteVisibilityVerticalRadius);
        assertEquals(25, config.worksiteVisibilityIntervalTicks);
        assertEquals(20, config.worksiteVisibilityMaxOverlayResults);
        assertEquals(
                VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                        | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                config.visualTargetMask);
    }

    @Test
    void schemaTwoFixtureAddsTechnicalTargetsWithoutReenablingNetherTargets() throws Exception {
        LocalFeatureConfig config = load("0.9.1-local.json");

        assertEquals(6, config.worksiteVisibilityHorizontalRadius);
        assertEquals(3, config.worksiteVisibilityVerticalRadius);
        assertEquals(20, config.worksiteVisibilityIntervalTicks);
        assertEquals(18, config.worksiteVisibilityMaxOverlayResults);
        assertEquals(
                VisualTargetSelectionPolicy.sanitizeMask(
                        8192 | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK),
                config.visualTargetMask);
    }

    @Test
    void retiredAnalyzerFieldsDoNotReenterCurrentConfig() throws Exception {
        LocalFeatureConfig config = load("0.9.2-local.json");

        assertTrue(config.lavaHighlightEnabled);
        assertTrue(config.fireVisibilityEnabled);
        assertEquals(7, config.lavaAnalyzerHorizontalRadius);
        assertEquals(2, config.lavaAnalyzerVerticalRadius);
        assertEquals(30, config.lavaAnalyzerIntervalTicks);
        assertEquals(21, config.lavaAnalyzerMaxOverlayResults);
        assertEquals(
                VisualTargetSelectionPolicy.sanitizeMask(1073739776),
                config.visualTargetMask);
    }

    @Test
    void futureSchemaKeepsKnownValuesAndIgnoresUnknownNestedFields() throws Exception {
        LocalFeatureConfig config = load("future-local.json");

        assertEquals(
                VisualTargetSelectionPolicy.sanitizeMask(1073750016),
                config.visualTargetMask);
        assertEquals(5, config.worksiteVisibilityHorizontalRadius);
        assertFalse(config.lavaHighlightEnabled);
    }

    private static LocalFeatureConfig load(String fixture) throws Exception {
        String document = Files.readString(FIXTURES.resolve(fixture));
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument(document), fixture);
        return config;
    }
}
