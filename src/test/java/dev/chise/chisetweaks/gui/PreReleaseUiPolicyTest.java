package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseUiPolicyTest {
    @Test
    void mainSurfaceAllowsEveryReleasedVisualFeature() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows()) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.MAIN, row), row.id());
        }

        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS));
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
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
    }

    @Test
    void visualFilterDetailsAndEditorsAreInteractiveAfterRelease() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS)) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS, row), row.id());
        }
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(PreReleaseUiPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
    }

    @Test
    void analyzerDetailControlsRemainInteractive() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS)) {
            assertTrue(PreReleaseUiPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.LAVA_DETAILS, row), row.id());
        }
    }
}
