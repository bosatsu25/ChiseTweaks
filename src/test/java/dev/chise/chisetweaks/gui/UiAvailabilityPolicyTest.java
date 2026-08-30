package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAvailabilityPolicyTest {
    @Test
    void booleanAndIntegerTweaksRemainInteractiveWhileInfoRowsAreReadOnly() {
        var controller = new ChiseTweaksSettingsController();
        for (var surface : ChiseTweaksSettingsController.Surface.values()) {
            for (ChiseTweaksSettingRowDefinition row : controller.rows(surface)) {
                if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) continue;
                if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) {
                    assertFalse(UiAvailabilityPolicy.isRowInteractive(surface, row), row.id());
                } else if (row.kind() != ChiseTweaksSettingRowDefinition.Kind.ACTION) {
                    assertTrue(UiAvailabilityPolicy.isRowInteractive(surface, row), row.id());
                }
            }
        }
    }

    @Test
    void actionsStayInsideTheirOwningTweaksGroup() {
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.BUILDER_HIGHLIGHTS,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));

        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.SCENE_FILTER,
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.SCENE_FILTER,
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));

        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.BUILDER_ASSIST,
                ChiseTweaksSettingRowDefinition.Action.SELECT_PATTERN_REFERENCE));
        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.BUILDER_ASSIST,
                ChiseTweaksSettingRowDefinition.Action.CLEAR_PATTERN_REFERENCE));

        assertTrue(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.INTEGRATIONS,
                ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE));
    }

    @Test
    void nullRowsAndActionsAreNotInteractive() {
        assertFalse(UiAvailabilityPolicy.isRowInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL, null));
        assertFalse(UiAvailabilityPolicy.isActionInteractive(
                ChiseTweaksSettingsController.Surface.VISUAL, null));
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
