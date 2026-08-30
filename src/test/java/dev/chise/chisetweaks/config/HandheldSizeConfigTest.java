package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HandheldSizeConfigTest {
    @Test
    void oldDocumentsAdoptSafeDisabledDefaults() {
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "fireVisibilityEnabled": true,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertFalse(config.handheldSizeEnabled);
        assertEquals(70, config.handheldBlockScalePercent);
        assertEquals(60, config.handheldItemScalePercent);
        assertEquals(75, config.handheldToolScalePercent);
    }

    @Test
    void explicitValuesPersistAndAreClampedToUsableBounds() {
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "handheldSizeEnabled": true,
                  "handheldBlockScalePercent": 1,
                  "handheldItemScalePercent": 500,
                  "handheldToolScalePercent": 83,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertTrue(config.handheldSizeEnabled);
        assertEquals(40, config.handheldBlockScalePercent);
        assertEquals(100, config.handheldItemScalePercent);
        assertEquals(83, config.handheldToolScalePercent);
    }

    @Test
    void malformedScaleTypesFailClosedToDefaults() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        config.handheldSizeEnabled = true;
        config.handheldBlockScalePercent = 90;

        assertFalse(config.replaceFromJsonDocument("""
                {
                  "handheldSizeEnabled": true,
                  "handheldBlockScalePercent": "70"
                }
                """));

        assertFalse(config.handheldSizeEnabled);
        assertEquals(70, config.handheldBlockScalePercent);
        assertEquals(60, config.handheldItemScalePercent);
        assertEquals(75, config.handheldToolScalePercent);
    }
}
