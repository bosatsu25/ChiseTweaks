package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FireVisibilityConfigTest {
    @Test
    void oldVisualConfigDefaultsFireVisibilityToOff() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "lavaHighlightEnabled": true,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));
        assertFalse(config.fireVisibilityEnabled);
        assertEquals(FireVisibilityPolicy.DEFAULT_SIZE_PRESET, config.fireVisibilitySizePreset);
    }

    @Test
    void explicitFireVisibilityOptInIsRestoredAfterRelease() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "fireVisibilityEnabled": true,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));
        assertTrue(config.fireVisibilityEnabled);
    }

    @Test
    void explicitSizePresetIsRestoredAndOutOfRangeValuesAreClamped() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "fireVisibilityEnabled": true,
                  "fireVisibilitySizePreset": 2,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));
        assertEquals(FireVisibilityPolicy.SIZE_SMALL, config.fireVisibilitySizePreset);

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "fireVisibilitySizePreset": 99,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));
        assertEquals(FireVisibilityPolicy.SIZE_SMALL, config.fireVisibilitySizePreset);
    }

    @Test
    void wrongFireVisibilityTypeIsRejectedAndRestoresSafeDefault() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        config.fireVisibilityEnabled = true;
        assertFalse(config.replaceFromJsonDocument("{\"fireVisibilityEnabled\":\"true\"}"));
        assertFalse(config.fireVisibilityEnabled);
    }
}
