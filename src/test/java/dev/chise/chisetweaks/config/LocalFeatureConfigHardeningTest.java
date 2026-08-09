package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class LocalFeatureConfigHardeningTest {
    @Test
    void malformedDocumentResetsEveryMutableSettingToSafeDefaults() {
        LocalFeatureConfig config = dirtyConfig();

        assertFalse(config.replaceFromJsonDocument("{not-json"));

        assertDefaults(config);
    }

    @Test
    void unsafeJsonDocumentResetsEveryMutableSettingToSafeDefaults() {
        LocalFeatureConfig config = dirtyConfig();

        assertFalse(config.replaceFromJsonDocument("{\"bad\\u202Ekey\":1}"));

        assertDefaults(config);
    }

    @Test
    void validOutOfRangeValuesAreClampedBeforeBecomingRuntimeState() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        String json = """
                {
                  "worksiteVisibilityHorizontalRadius": 999,
                  "worksiteVisibilityVerticalRadius": -10,
                  "worksiteVisibilityIntervalTicks": 0,
                  "worksiteVisibilityMaxResults": 999,
                  "worksiteVisibilityMaxOverlayResults": 999,
                  "pumpkinScaffoldPlacementRange": 999
                }
                """;

        assertTrue(config.replaceFromJsonDocument(json));
        assertEquals(WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
                config.worksiteVisibilityHorizontalRadius);
        assertEquals(WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
                config.worksiteVisibilityVerticalRadius);
        assertEquals(WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
                config.worksiteVisibilityIntervalTicks);
        assertEquals(WorksiteVisibilityBudgetPolicy.LEGACY_MAX_RESULTS,
                config.worksiteVisibilityMaxResults);
        assertEquals(WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
                config.worksiteVisibilityMaxOverlayResults);
        assertEquals(PumpkinScaffoldPolicy.MAX_PLACEMENT_RANGE,
                config.pumpkinScaffoldPlacementRange);
    }

    @Test
    void unknownFieldsDoNotChangeKnownSettings() {
        LocalFeatureConfig config = new LocalFeatureConfig();

        assertTrue(config.replaceFromJsonDocument("{\"futureSetting\":true}"));

        assertDefaults(config);
    }

    private static LocalFeatureConfig dirtyConfig() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        config.lavaHighlightEnabled = true;
        config.lavaHighlightSource = false;
        config.lavaHighlightFlowing = false;
        config.lavaSourceColor = 1;
        config.lavaFlowingColor = 2;
        config.worksiteVisibilityHorizontalRadius = 8;
        config.worksiteVisibilityVerticalRadius = 5;
        config.worksiteVisibilityIntervalTicks = 100;
        config.worksiteVisibilityMaxResults = 8;
        config.worksiteVisibilityMaxOverlayResults = 24;
        config.worksiteVisibilityWorldOverlay = false;
        config.worksiteVisibilityExclusiveMode = true;
        config.pumpkinScaffoldPlacementRange = 1;
        return config;
    }

    private static void assertDefaults(LocalFeatureConfig config) {
        assertFalse(config.lavaHighlightEnabled);
        assertTrue(config.lavaHighlightSource);
        assertTrue(config.lavaHighlightFlowing);
        assertEquals(0xFFFF3B30, config.lavaSourceColor);
        assertEquals(0xFFFF9500, config.lavaFlowingColor);
        assertEquals(5, config.worksiteVisibilityHorizontalRadius);
        assertEquals(3, config.worksiteVisibilityVerticalRadius);
        assertEquals(10, config.worksiteVisibilityIntervalTicks);
        assertEquals(6, config.worksiteVisibilityMaxResults);
        assertEquals(12, config.worksiteVisibilityMaxOverlayResults);
        assertTrue(config.worksiteVisibilityWorldOverlay);
        assertFalse(config.worksiteVisibilityExclusiveMode);
        assertEquals(PumpkinScaffoldPolicy.DEFAULT_PLACEMENT_RANGE,
                config.pumpkinScaffoldPlacementRange);
    }
}
