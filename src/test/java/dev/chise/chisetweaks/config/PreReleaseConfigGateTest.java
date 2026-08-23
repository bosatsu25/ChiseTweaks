package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseConfigGateTest {
    @Test
    void featureConfigRestoresAllReleasedFeatureSwitches() {
        try {
            for (FeatureSwitch feature : FeatureSwitches.VALUES) {
                feature.setBooleanValueSilently(true);
                assertTrue(feature.getBooleanValue(), feature.getName());
            }
        } finally {
            for (FeatureSwitch feature : FeatureSwitches.VALUES) {
                feature.setBooleanValueSilently(false);
            }
        }
    }

    @Test
    void releasedLavaAndLowFireSettingsCanBothBeRestored() {
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
        assertTrue(config.fireVisibilityEnabled);
    }
}
