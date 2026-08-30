package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HiddenBlockAnalyzerConfigIsolationTest {
    @Test
    void legacySharedScanValuesSeedOccludedHighlightBudget() {
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

        assertEquals(7, config.occludedHighlightHorizontalRadius);
        assertEquals(4, config.occludedHighlightVerticalRadius);
        assertEquals(35, config.occludedHighlightIntervalTicks);
        assertEquals(19, config.occludedHighlightMaxOverlayResults);
    }

    @Test
    void occludedHighlightBudgetUsesSharedBoundedSafetyPolicy() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "occludedHighlightHorizontalRadius": 999,
                  "occludedHighlightVerticalRadius": -50,
                  "occludedHighlightIntervalTicks": 9999,
                  "occludedHighlightMaxOverlayResults": 999,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """));

        assertEquals(8, config.occludedHighlightHorizontalRadius);
        assertEquals(1, config.occludedHighlightVerticalRadius);
        assertEquals(100, config.occludedHighlightIntervalTicks);
        assertEquals(24, config.occludedHighlightMaxOverlayResults);
    }
}
