package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigMigrationDowngradeTest {
    @Test
    void futureSchemaKeepsKnownExplicitBitsAndDropsUnknownBits() {
        int known = VisualTargetSelectionPolicy.Target.MATERIAL_DIAMOND_ORE.bitMask();
        int unknown = 1 << 30;
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "visualTargetMask": %d,
                  "visualTargetSchemaVersion": 99,
                  "futureField": true
                }
                """.formatted(known | unknown)));

        assertEquals(known, config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void preAnalyzerConfigMigratesWorksiteBudgetsIntoLavaBudgets() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityHorizontalRadius": 7,
                  "worksiteVisibilityVerticalRadius": 2,
                  "worksiteVisibilityIntervalTicks": 35,
                  "worksiteVisibilityMaxOverlayResults": 19,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertEquals(7, config.lavaAnalyzerHorizontalRadius);
        assertEquals(2, config.lavaAnalyzerVerticalRadius);
        assertEquals(35, config.lavaAnalyzerIntervalTicks);
        assertEquals(19, config.lavaAnalyzerMaxOverlayResults);
    }

    @Test
    void explicitLavaBudgetsOverrideLegacyWorksiteFallbacks() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityHorizontalRadius": 7,
                  "worksiteVisibilityVerticalRadius": 2,
                  "worksiteVisibilityIntervalTicks": 35,
                  "worksiteVisibilityMaxOverlayResults": 19,
                  "lavaAnalyzerHorizontalRadius": 3,
                  "lavaAnalyzerVerticalRadius": 1,
                  "lavaAnalyzerIntervalTicks": 15,
                  "lavaAnalyzerMaxOverlayResults": 9,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertEquals(3, config.lavaAnalyzerHorizontalRadius);
        assertEquals(1, config.lavaAnalyzerVerticalRadius);
        assertEquals(15, config.lavaAnalyzerIntervalTicks);
        assertEquals(9, config.lavaAnalyzerMaxOverlayResults);
    }

    @Test
    void veryOldOrNegativeSchemaAddsEveryIntroducedTargetFamily() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": -5
                }
                """));
        assertEquals(
                VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                        | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                config.visualTargetMask);
    }
}
