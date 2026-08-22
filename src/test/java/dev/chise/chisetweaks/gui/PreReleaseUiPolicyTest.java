package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseUiPolicyTest {
    @Test
    void mainSurfaceAllowsReleasedVisualFeaturesAndKeepsUnreleasedFeaturesLocked() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        for (ChiseTweaksSettingRowDefinition row : rows) {
            boolean expected = switch (row.id()) {
                case "header.highlight", "header.visualFilter", "header.visibilityImprovement",
                        "materials", "kelp", "glass", "lava", "ancientDebrisAnalyzer" -> true;
                default -> false;
            };
            assertTrue(
                    PreReleaseUiPolicy.isRowInteractive(ChiseTweaksSettingsController.Surface.MAIN, row) == expected,
                    row.id());
        }
        assertFalse(PreReleaseUiPolicy.isHighlightBulkInteractive());
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS));
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
    }

    @Test
    void releasedAncientDebrisAnalyzerCannotRegressToADisabledMainToggle() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition ancient = controller.rows().stream()
                .filter(row -> "ancientDebrisAnalyzer".equals(row.id()))
                .findFirst()
                .orElseThrow();

        assertTrue(PreReleaseUiPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ancient));
    }

    @Test
    void highlightDetailsOnlyAllowOreSpecificControls() {
        var controller = new ChiseTweaksSettingsController(true);
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS)) {
            boolean expected = row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                    || row.id().equals("oreMotion")
                    || row.id().equals("moddedOreTargets")
                    || row.id().startsWith("visualTargetMaterial");
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS, row) == expected, row.id());
        }
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS));
    }

    @Test
    void analyzerDetailControlsAreInteractiveForBothReleasedAnalyzers() {
        var controller = new ChiseTweaksSettingsController(true);
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS)) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.LAVA_DETAILS, row), row.id());
        }
    }
}
