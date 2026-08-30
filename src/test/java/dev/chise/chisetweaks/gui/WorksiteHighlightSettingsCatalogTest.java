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
    void technicalVisualizationOwnsTraceAppearanceAndSharedOverlayBudget() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.ANALYZER);

        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                row(rows, "highlightDimensionPresets").booleanConfig());
        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                row(rows, "fineThreadColor").integerConfig());
        assertSame(LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                row(rows, "fineThreadOpacity").integerConfig());
        assertEquals(1, row(rows, "fineThreadColor").step());
        assertEquals(5, row(rows, "fineThreadOpacity").step());
    }

    @Test
    void technicalTargetsAndOccludedTargetsBelongToDifferentProductGroups() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> highlights = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        List<ChiseTweaksSettingRowDefinition> technical = controller.rows(
                ChiseTweaksSettingsController.Surface.ANALYZER);

        assertTrue(technical.stream().anyMatch(row -> row.id().equals("visualTargetTechnicalTripwire")));
        assertTrue(technical.stream().anyMatch(row -> row.id().equals("visualTargetTechnicalTripwireHook")));
        assertFalse(technical.stream().anyMatch(row -> row.id().startsWith("visualTargetHidden")));

        assertTrue(highlights.stream().anyMatch(row -> row.id().equals("visualTargetHiddenBlueIce")));
        assertTrue(highlights.stream().anyMatch(row -> row.id().equals("visualTargetHiddenDeadCoral")));
        assertTrue(highlights.stream().anyMatch(row -> row.id().equals("visualTargetHiddenPowderSnow")));
        assertTrue(highlights.stream().anyMatch(row -> row.id().equals("visualTargetHiddenSculkCatalyst")));
    }

    @Test
    void technicalResetRestoresTraceControlsAndTargets() {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.worksiteVisibilityDimensionPresetsEnabled = true;
        config.fineThreadTraceColorPreset = 7;
        config.fineThreadTraceOpacityPercent = 20;
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE, false);
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE_HOOK, false);

        var controller = new ChiseTweaksSettingsController();
        controller.reset(ChiseTweaksSettingsController.Surface.ANALYZER);

        assertFalse(config.worksiteVisibilityDimensionPresetsEnabled);
        assertEquals(-1, config.fineThreadTraceColorPreset);
        assertEquals(100, config.fineThreadTraceOpacityPercent);
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE));
        assertTrue(VisualTargetSelectionPolicy.isEnabled(
                config.visualTargetMask, Target.TECHNICAL_TRIPWIRE_HOOK));
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
