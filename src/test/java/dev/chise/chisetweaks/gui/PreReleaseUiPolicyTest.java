package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseUiPolicyTest {
    @Test
    void mainSurfaceAllowsReleasedVisualFeaturesAndKeepsUnreleasedFeaturesLocked() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        for (ChiseTweaksSettingRowDefinition row : rows) {
            boolean expected = switch (row.id()) {
                case "header.highlight", "header.visualFilter", "header.analyzer", "header.visibilityImprovement",
                        "materials", "thread", "hidden", "kelp", "glass", "lava", "ancientDebrisAnalyzer" -> true;
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
    void releasedWorksiteHighlightsCannotRegressToDisabledMainToggles() {
        var controller = new ChiseTweaksSettingsController();
        for (String id : List.of("thread", "hidden", "ancientDebrisAnalyzer")) {
            ChiseTweaksSettingRowDefinition row = controller.rows().stream()
                    .filter(candidate -> id.equals(candidate.id()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.MAIN,
                    row), id);
        }
    }

    @Test
    void analyzerActionLivesOnAnalyzerHeaderInsteadOfGeneralVisibilityHeader() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition analyzer = controller.rows().stream()
                .filter(row -> "header.analyzer".equals(row.id()))
                .findFirst()
                .orElseThrow();
        ChiseTweaksSettingRowDefinition visibility = controller.rows().stream()
                .filter(row -> "header.visibilityImprovement".equals(row.id()))
                .findFirst()
                .orElseThrow();

        assertTrue(PreReleaseUiPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.MAIN, analyzer));
        assertTrue(PreReleaseUiPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.MAIN, visibility));
        assertTrue(analyzer.action() == ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS);
        assertTrue(visibility.action() == null);
    }

    @Test
    void highlightDetailsExposeReleasedOreAndWorksiteControls() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS)) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS, row), row.id());
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
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS)) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.LAVA_DETAILS, row), row.id());
        }
    }
}
