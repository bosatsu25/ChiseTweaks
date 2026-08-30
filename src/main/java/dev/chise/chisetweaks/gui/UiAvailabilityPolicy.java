package dev.chise.chisetweaks.gui;

/** Keeps UI action availability aligned with the seven Tweaks product groups. */
final class UiAvailabilityPolicy {
    private UiAvailabilityPolicy() {}

    static boolean isRowInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition row) {
        if (row == null) return false;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) return true;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) return false;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.ACTION) {
            return isActionInteractive(surface, row.action());
        }
        return true;
    }

    static boolean isActionInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition.Action action) {
        if (action == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.VISUAL
                : surface;
        return switch (resolved) {
            case BUILDER_HIGHLIGHTS ->
                    action == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT;
            case SCENE_FILTER ->
                    action == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER
                    || action == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER;
            case BUILDER_ASSIST ->
                    action == ChiseTweaksSettingRowDefinition.Action.SELECT_PATTERN_REFERENCE
                    || action == ChiseTweaksSettingRowDefinition.Action.CLEAR_PATTERN_REFERENCE;
            case INTEGRATIONS ->
                    action == ChiseTweaksSettingRowDefinition.Action.EDIT_LITEMATICA_PICK_REDIRECT
                    || action == ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKERMORE_AUTO_PICK_GUARD
                    || action == ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKEROO_TOOL_SWITCH_GUARD
                    || action == ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE;
            case VISUAL, TECHNICAL_VISUALIZATION, WORKFLOW -> false;
        };
    }
}
