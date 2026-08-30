package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HiddenBlockAnalyzerConfigIsolationTest {
    @Test
    void legacySharedScanValuesSeedDedicatedHiddenAnalyzerSettings() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityHorizontalRadius": 7,
                  "worksiteVisibilityVerticalRadius": 4,
                  "worksiteVisibilityIntervalTicks": 35,
                  "worksiteVisibilityMaxOverlayResults": 19,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertEquals(7, config.hiddenAnalyzerHorizontalRadius);
        assertEquals(4, config.hiddenAnalyzerVerticalRadius);
        assertEquals(35, config.hiddenAnalyzerIntervalTicks);
        assertEquals(19, config.hiddenAnalyzerMaxOverlayResults);
    }

    @Test
    void dedicatedHiddenAnalyzerSettingsUseTheSharedBoundedSafetyPolicy() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "hiddenAnalyzerHorizontalRadius": 999,
                  "hiddenAnalyzerVerticalRadius": -50,
                  "hiddenAnalyzerIntervalTicks": 9999,
                  "hiddenAnalyzerMaxOverlayResults": 999,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertEquals(8, config.hiddenAnalyzerHorizontalRadius);
        assertEquals(1, config.hiddenAnalyzerVerticalRadius);
        assertEquals(100, config.hiddenAnalyzerIntervalTicks);
        assertEquals(24, config.hiddenAnalyzerMaxOverlayResults);
    }
}
