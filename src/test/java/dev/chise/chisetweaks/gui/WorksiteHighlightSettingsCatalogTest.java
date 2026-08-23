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
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY.resetToDefault();
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        config.visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    @Test
    void highlightDetailsExposePhaseTwoAppearanceAndDimensionControls() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                row(rows, "highlightDimensionPresets").booleanConfig());
        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                row(rows, "fineThreadColor").integerConfig());
        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                row(rows, "fineThreadOpacity").integerConfig());
        assertSame(LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                row(rows, "hiddenSurfaceColor").integerConfig());
        assertSame(LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                row(rows, "hiddenSurfaceOpacity").integerConfig());

        assertEquals(1, row(rows, "fineThreadColor").step());
        assertEquals(5, row(rows, "fineThreadOpacity").step());
        assertEquals(1, row(rows, "hiddenSurfaceColor").step());
        assertEquals(5, row(rows, "hiddenSurfaceOpacity").step());
    }

    @Test
    void fineThreadAndHiddenSurfaceTargetSelectionRemainAvailable() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetTechnicalTripwire")));
        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetTechnicalTripwireHook")));
        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetHiddenBlueIce")));
        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetHiddenDeadCoral")));
        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetHiddenPowderSnow")));
        assertTrue(rows.stream().anyMatch(row -> row.id().equals("visualTargetHiddenSculkCatalyst")));
    }

    @Test
    void highlightResetRestoresPhaseTwoControlsAndTraceTargetsToSafeDefaults() {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.worksiteVisibilityDimensionPresetsEnabled = true;
        config.fineThreadTraceColorPreset = 7;
        config.fineThreadTraceOpacityPercent = 20;
        config.hiddenSurfaceTraceColorPreset = 3;
        config.hiddenSurfaceTraceOpacityPercent = 40;
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE, false);
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE_HOOK, false);
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.HIDDEN_BLUE_ICE, false);

        var controller = new ChiseTweaksSettingsController();
        controller.reset(ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertFalse(config.worksiteVisibilityDimensionPresetsEnabled);
        assertEquals(-1, config.fineThreadTraceColorPreset);
        assertEquals(100, config.fineThreadTraceOpacityPercent);
        assertEquals(-1, config.hiddenSurfaceTraceColorPreset);
        assertEquals(100, config.hiddenSurfaceTraceOpacityPercent);
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE_HOOK));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.HIDDEN_BLUE_ICE));
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
