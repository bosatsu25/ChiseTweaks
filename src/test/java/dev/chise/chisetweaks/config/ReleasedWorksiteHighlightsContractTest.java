package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleasedWorksiteHighlightsContractTest {
    @AfterEach
    void resetSwitches() {
        FeatureSwitches.FINE_THREAD_TRACE.setBooleanValueSilently(false);
        FeatureSwitches.HIDDEN_SURFACE_TRACE.setBooleanValueSilently(false);
    }

    @Test
    void fineThreadAndHiddenSurfaceAreOptInButCanBeEnabledIndependently() {
        assertFalse(FeatureSwitches.FINE_THREAD_TRACE.getDefaultBooleanValue());
        assertFalse(FeatureSwitches.HIDDEN_SURFACE_TRACE.getDefaultBooleanValue());

        FeatureSwitches.FINE_THREAD_TRACE.setBooleanValueSilently(true);
        assertTrue(FeatureSwitches.FINE_THREAD_TRACE.getBooleanValue());
        assertFalse(FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue());

        FeatureSwitches.HIDDEN_SURFACE_TRACE.setBooleanValueSilently(true);
        assertTrue(FeatureSwitches.FINE_THREAD_TRACE.getBooleanValue());
        assertTrue(FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue());
    }

    @Test
    void releasedTargetsRemainExplicitAndDoNotExpandToUnrelatedBlocks() {
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                BlockInspectionPolicy.classify("minecraft:tripwire"));
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                BlockInspectionPolicy.classify("minecraft:tripwire_hook"));
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionPolicy.classify("minecraft:powder_snow"));
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionPolicy.classify("minecraft:blue_ice"));
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionPolicy.classify("minecraft:sculk_catalyst"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:snow_block"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:sculk"));
    }

    @Test
    void defaultWorksiteScanStaysInsideTheExistingPerformanceBudget() {
        LocalFeatureConfig defaults = new LocalFeatureConfig();

        assertEquals(5, defaults.worksiteVisibilityHorizontalRadius);
        assertEquals(3, defaults.worksiteVisibilityVerticalRadius);
        assertEquals(10, defaults.worksiteVisibilityIntervalTicks);
        assertEquals(12, defaults.worksiteVisibilityMaxOverlayResults);
        assertTrue(WorksiteVisibilityBudgetPolicy.maximumBlocksFor(
                defaults.worksiteVisibilityHorizontalRadius,
                defaults.worksiteVisibilityVerticalRadius) <= 1024);
        assertTrue(defaults.worksiteVisibilityMaxOverlayResults
                <= WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS);
        assertTrue(defaults.worksiteVisibilityIntervalTicks
                >= WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS);
    }
}
