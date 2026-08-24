package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAvailabilityPolicyTest {
    @Test
    void releasedSettingTabsAllowTheirInteractiveRows() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingsController.Surface surface : new ChiseTweaksSettingsController.Surface[]{
                ChiseTweaksSettingsController.Surface.HIGHLIGHT,
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER,
                ChiseTweaksSettingsController.Surface.ANALYZER,
                ChiseTweaksSettingsController.Surface.VISIBILITY}) {
            for (ChiseTweaksSettingRowDefinition row : controller.rows(surface)) {
                if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) continue;
                assertTrue(UiAvailabilityPolicy.isRowInteractive(surface, row), row.id());
            }
        }
    }

    @Test
    void helpRowsAreDisplayOnly() {
        var controller = new ChiseTweaksSettingsController();
        for (ChiseTweaksSettingRowDefinition row : controller.rows(ChiseTweaksSettingsController.Surface.HELP)) {
            if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) continue;
            assertFalse(UiAvailabilityPolicy.isRowInteractive(
                    ChiseTweaksSettingsController.Surface.HELP, row), row.id());
        }
    }

    @Test
    void tabActionsStayInsideTheirOwningCategory() {
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.ANALYZER,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
    }

    @Test
    void nullRowsAndActionsAreNotInteractive() {
        assertFalse(UiAvailabilityPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT, null));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT, null));
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
