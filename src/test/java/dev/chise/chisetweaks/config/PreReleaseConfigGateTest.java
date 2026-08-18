package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseConfigGateTest {
    @Test
    void featureConfigCannotEnableUnreleasedFeatureSwitches() {
        try {
            for (FeatureSwitch feature : FeatureSwitches.VALUES) {
                feature.setBooleanValueSilently(true);
            }

            assertTrue(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
            assertTrue(FeatureSwitches.GLASS_INSPECTION.getBooleanValue());
            assertTrue(FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue());
            assertFalse(FeatureSwitches.NETHER_PALETTE.getBooleanValue());
            assertFalse(FeatureSwitches.FINE_THREAD_TRACE.getBooleanValue());
            assertFalse(FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue());
            assertFalse(FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue());
            assertFalse(FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue());
        } finally {
            for (FeatureSwitch feature : FeatureSwitches.VALUES) {
                feature.setBooleanValueSilently(false);
            }
        }
    }

    @Test
    void releasedLavaSettingCanBeRestoredWhileFireRemainsLocked() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "lavaHighlightEnabled": true,
                  "fireVisibilityEnabled": true,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertTrue(config.lavaHighlightEnabled);
        assertFalse(config.fireVisibilityEnabled);
    }
}
