package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientDebrisConfigPersistenceTest {
    @Test
    void localConfigReadsAllAncientDebrisAnalyzerFields() {
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "ancientDebrisAnalyzerEnabled": true,
                  "ancientDebrisAnalyzerRangeBlocks": 128,
                  "ancientDebrisAnalyzerMaxMarkers": 96,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertTrue(config.ancientDebrisAnalyzerEnabled);
        assertEquals(128, config.ancientDebrisAnalyzerRangeBlocks);
        assertEquals(96, config.ancientDebrisAnalyzerMaxMarkers);
    }

    @Test
    void localConfigStillIgnoresRemovedAndUnknownFields() {
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "ancientDebrisAnalyzerEnabled": true,
                  "ancientDebrisAnalyzerRangeBlocks": 80,
                  "pumpkinScaffoldPlacementRange": 7,
                  "removedAnalyzerColor": 12345,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertTrue(config.ancientDebrisAnalyzerEnabled);
        assertEquals(80, config.ancientDebrisAnalyzerRangeBlocks);
    }
}
