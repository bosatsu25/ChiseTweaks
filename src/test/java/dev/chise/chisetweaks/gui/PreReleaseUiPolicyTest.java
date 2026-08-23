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
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS));
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
    void sectionHeadersStayVisibleWhileEachSettingsActionHasIndependentAvailability() {
        var controller = new ChiseTweaksSettingsController();
        for (String id : List.of("header.highlight", "header.visualFilter", "header.analyzer")) {
            ChiseTweaksSettingRowDefinition header = controller.rows().stream()
                    .filter(row -> id.equals(row.id()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.MAIN, header), id);
        }

        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS));
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS));
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
    void visualFilterDetailSurfaceRemainsFailClosedUntilBuilderFocusRelease() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS)) {
            if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                assertTrue(PreReleaseUiPolicy.isRowInteractive(
                        ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS, row), row.id());
            } else {
                assertFalse(PreReleaseUiPolicy.isRowInteractive(
                        ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS, row), row.id());
            }
        }
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertFalse(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
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
