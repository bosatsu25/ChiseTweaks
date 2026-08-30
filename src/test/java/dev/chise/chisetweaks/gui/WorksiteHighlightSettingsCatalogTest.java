package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorksiteHighlightSettingsCatalogTest {
    @AfterEach
    void resetSharedConfig() {
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        config.visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    @Test
    void builderHighlightsOwnMaterialHiddenAndOccludedControls() {
        var rows = new ChiseTweaksSettingsController().rows(
                ChiseTweaksSettingsController.Surface.BUILDER_HIGHLIGHTS);

        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                row(rows, "highlightDimensionPresets").booleanConfig());
        assertSame(LocalFeatureSettings.OCCLUDED_HIGHLIGHT_HORIZONTAL_RADIUS,
                row(rows, "occludedRange").integerConfig());
        assertTrue(rows.stream().anyMatch(r -> r.id().startsWith("visualTargetMaterial")));
        assertTrue(rows.stream().anyMatch(r -> r.id().startsWith("visualTargetHidden")));
        assertFalse(rows.stream().anyMatch(r -> r.id().startsWith("visualTargetTechnical")));
    }

    @Test
    void technicalTargetsMoveToTechnicalVisualization() {
        var rows = new ChiseTweaksSettingsController().rows(
                ChiseTweaksSettingsController.Surface.TECHNICAL_VISUALIZATION);

        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                row(rows, "fineThreadColor").integerConfig());
        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                row(rows, "fineThreadOpacity").integerConfig());
        assertTrue(rows.stream().anyMatch(r -> r.id().equals("visualTargetTechnicalTripwire")));
        assertTrue(rows.stream().anyMatch(r -> r.id().equals("visualTargetTechnicalTripwireHook")));
    }

    @Test
    void technicalResetRestoresTraceTargetsOnly() {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.fineThreadTraceColorPreset = 7;
        config.fineThreadTraceOpacityPercent = 20;
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE, false);

        var controller = new ChiseTweaksSettingsController();
        controller.reset(ChiseTweaksSettingsController.Surface.TECHNICAL_VISUALIZATION);

        assertEquals(-1, config.fineThreadTraceColorPreset);
        assertEquals(100, config.fineThreadTraceOpacityPercent);
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE));
    }

    private static ChiseTweaksSettingRowDefinition row(
            List<ChiseTweaksSettingRowDefinition> rows,
            String id) {
        return rows.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row: " + id));
    }
}
