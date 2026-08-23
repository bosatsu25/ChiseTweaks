package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAvailabilityPolicyTest {
    @Test
    void mainSurfaceAllowsAllReleasedRows() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows()) {
            assertTrue(UiAvailabilityPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.MAIN, row), row.id());
        }
    }

    @Test
    void detailSurfacesKeepTheirActionBoundaries() {
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
    }

    @Test
    void nullRowsAndActionsAreNotInteractive() {
        assertFalse(UiAvailabilityPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.MAIN, null));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.MAIN, null));
    }

    @Test
    void settingRowIdsRejectAmbiguousValues() {
        assertThrows(NullPointerException.class, () -> SettingRowId.of(null));
        assertThrows(IllegalArgumentException.class, () -> SettingRowId.of(""));
        assertThrows(IllegalArgumentException.class, () -> SettingRowId.of(" chestVisibility "));
        assertThrows(IllegalArgumentException.class, () -> SettingRowId.of("chest visibility"));
        assertTrue(SettingRowId.of("visualTargetMaterialOre").startsWith("visualTargetMaterial"));
    }
}
