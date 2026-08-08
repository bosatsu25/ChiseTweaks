package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualTargetConfigPersistenceTest {
    @Test
    void legacyDocumentsWithoutTargetMaskKeepAllTargetsEnabled() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"worksiteVisibilityHorizontalRadius\":5}"));
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
    }

    @Test
    void persistedTargetMaskKeepsIndividualSelections() {
        int mask = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_DIAMOND_ORE,
                false);
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"visualTargetMask\":" + mask + "}"));
        assertEquals(mask, config.visualTargetMask);
    }

    @Test
    void unknownUpperBitsAreRemovedDuringSanitization() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("{\"visualTargetMask\":2147483647}"));
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, config.visualTargetMask);
    }
}
