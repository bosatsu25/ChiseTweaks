package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.policy.WorksiteHighlightProfilePolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorksiteHighlightConfigTest {
    @AfterEach
    void restoreSharedConfigDefaults() {
        LocalFeatureConfig.getInstance().resetToDefaults();
    }

    @Test
    void newTraceAppearanceFieldsLoadAndClampThroughTheSecureDocumentPath() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityDimensionPresetsEnabled": true,
                  "fineThreadTraceColorPreset": 99,
                  "fineThreadTraceOpacityPercent": 1,
                  "hiddenSurfaceTraceColorPreset": 4,
                  "hiddenSurfaceTraceOpacityPercent": 999,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertTrue(config.worksiteVisibilityDimensionPresetsEnabled);
        assertEquals(WorksiteHighlightProfilePolicy.MAX_COLOR_PRESET,
                config.fineThreadTraceColorPreset);
        assertEquals(WorksiteHighlightProfilePolicy.MIN_OPACITY_PERCENT,
                config.fineThreadTraceOpacityPercent);
        assertEquals(4, config.hiddenSurfaceTraceColorPreset);
        assertEquals(WorksiteHighlightProfilePolicy.MAX_OPACITY_PERCENT,
                config.hiddenSurfaceTraceOpacityPercent);
    }

    @Test
    void traceAppearanceDefaultsPreserveTheExistingReleasedVisuals() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertFalse(config.worksiteVisibilityDimensionPresetsEnabled);
        assertEquals(WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET,
                config.fineThreadTraceColorPreset);
        assertEquals(WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT,
                config.fineThreadTraceOpacityPercent);
        assertEquals(WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET,
                config.hiddenSurfaceTraceColorPreset);
        assertEquals(WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT,
                config.hiddenSurfaceTraceOpacityPercent);
    }

    @Test
    void formattedSettingsExposeColorAndOpacityWithoutRawSentinelValues() {
        LocalFeatureConfig shared = LocalFeatureConfig.getInstance();
        shared.resetToDefaults();

        assertEquals("AUTO", LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.getFormattedValue());
        assertEquals("100%", LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.getFormattedValue());

        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.setIntegerValueSilently(1);
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.setIntegerValueSilently(65);
        assertEquals("#6FE7F7", LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.getFormattedValue());
        assertEquals("65%", LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.getFormattedValue());
    }
}
