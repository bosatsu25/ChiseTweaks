package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

final class WorksiteHighlightSettingsCatalogTest {
    @AfterEach
    void resetSharedConfig() {
        LocalFeatureConfig.getInstance().resetToDefaults();
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
    void hiddenSurfaceTargetSelectionRemainsAvailableAlongsideAppearanceControls() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertFalse(rows.stream().noneMatch(row -> row.id().equals("visualTargetHiddenBlueIce")));
        assertFalse(rows.stream().noneMatch(row -> row.id().equals("visualTargetHiddenDeadCoral")));
        assertFalse(rows.stream().noneMatch(row -> row.id().equals("visualTargetHiddenPowderSnow")));
        assertFalse(rows.stream().noneMatch(row -> row.id().equals("visualTargetHiddenSculkCatalyst")));
    }

    @Test
    void highlightResetRestoresPhaseTwoControlsToSafeDefaults() {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.worksiteVisibilityDimensionPresetsEnabled = true;
        config.fineThreadTraceColorPreset = 7;
        config.fineThreadTraceOpacityPercent = 20;
        config.hiddenSurfaceTraceColorPreset = 3;
        config.hiddenSurfaceTraceOpacityPercent = 40;

        var controller = new ChiseTweaksSettingsController();
        controller.reset(ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertFalse(config.worksiteVisibilityDimensionPresetsEnabled);
        assertEquals(-1, config.fineThreadTraceColorPreset);
        assertEquals(100, config.fineThreadTraceOpacityPercent);
        assertEquals(-1, config.hiddenSurfaceTraceColorPreset);
        assertEquals(100, config.hiddenSurfaceTraceOpacityPercent);
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
