package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

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
    }

    @Test
    void explicitFireVisibilityOptInPersistsThroughStrictOverlay() {
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
    void wrongFireVisibilityTypeIsRejectedAndRestoresSafeDefault() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        config.fireVisibilityEnabled = true;
        assertFalse(config.replaceFromJsonDocument("{\"fireVisibilityEnabled\":\"true\"}"));
        assertFalse(config.fireVisibilityEnabled);
    }
}
