package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.FeatureSwitches;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleasedHighlightBulkRegressionTest {
    @AfterEach
    void resetReleasedHighlights() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.setBooleanValueSilently(false);
        FeatureSwitches.GLASS_INSPECTION.setBooleanValueSilently(false);
        FeatureSwitches.KELP_HIGHLIGHT.setBooleanValueSilently(false);
    }

    @Test
    void lockedHighlightsDoNotPreventReleasedBulkStateFromTurningOffAgain() {
        FeatureSwitches.NETHER_PALETTE.setBooleanValueSilently(false);
        FeatureSwitches.FINE_THREAD_TRACE.setBooleanValueSilently(false);
        FeatureSwitches.HIDDEN_SURFACE_TRACE.setBooleanValueSilently(false);
        var controller = new ChiseTweaksSettingsController();

        assertTrue(controller.shouldTurnHighlightBulkOn());
        controller.toggleHighlightBulk();

        assertTrue(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
        assertTrue(FeatureSwitches.GLASS_INSPECTION.getBooleanValue());
        assertTrue(FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue());
        assertFalse(FeatureSwitches.NETHER_PALETTE.getBooleanValue());
        assertFalse(FeatureSwitches.FINE_THREAD_TRACE.getBooleanValue());
        assertFalse(FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue());
        assertFalse(controller.shouldTurnHighlightBulkOn());

        controller.toggleHighlightBulk();
        assertFalse(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
        assertFalse(FeatureSwitches.GLASS_INSPECTION.getBooleanValue());
        assertFalse(FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue());
        assertTrue(controller.shouldTurnHighlightBulkOn());
    }
}
