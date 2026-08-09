package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualTargetConfigPersistenceTest {
    @Test
    void legacyDocumentsWithoutTargetMaskKeepAllTargetsEnabled() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"worksiteVisibilityHorizontalRadius\":5}"));
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void persistedLegacyMaskGainsNewNetherTargetsOnce() {
        int legacyMask = (1 << 25) - 1;
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"visualTargetMask\":" + legacyMask + "}"));
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void currentSchemaKeepsExplicitlyDisabledNewTarget() {
        int mask = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_NETHER_QUARTZ_ORE,
                false);
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument(
                "{\"visualTargetMask\":" + mask
                        + ",\"visualTargetSchemaVersion\":"
                        + VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION + "}"));
        assertEquals(mask, config.visualTargetMask);
        assertFalse(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask,
                Target.MATERIAL_NETHER_QUARTZ_ORE));
    }

    @Test
    void persistedTargetMaskKeepsIndividualLegacySelections() {
        int mask = VisualTargetSelectionPolicy.withEnabled(
                (1 << 25) - 1,
                Target.MATERIAL_DIAMOND_ORE,
                false);
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"visualTargetMask\":" + mask + "}"));
        assertFalse(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask,
                Target.MATERIAL_DIAMOND_ORE));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask,
                Target.MATERIAL_CRYING_OBSIDIAN));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask,
                Target.MATERIAL_NETHER_GOLD_ORE));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask,
                Target.MATERIAL_NETHER_QUARTZ_ORE));
    }

    @Test
    void unknownUpperBitsAreRemovedDuringSanitization() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument(
                "{\"visualTargetMask\":2147483647,\"visualTargetSchemaVersion\":2}"));
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
    }
}
