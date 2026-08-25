package dev.chise.chisetweaks.gui;

/** 現行リリースのUI操作可否をfeature runtimeの利用可否から分離して判定する。 */
final class UiAvailabilityPolicy {
    private static final String MATERIAL_PREFIX = "visualTargetMaterial";
    private static final String TECHNICAL_PREFIX = "visualTargetTechnical";
    private static final String HIDDEN_PREFIX = "visualTargetHidden";

    private UiAvailabilityPolicy() {}

    static boolean isRowInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition row) {
        if (row == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.HIGHLIGHT
                : surface;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) return true;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) return false;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.ACTION) {
            return isActionInteractive(resolved, row.action());
        }
        if (resolved == ChiseTweaksSettingsController.Surface.HIGHLIGHT) {
            return isReleasedHighlightRow(row.settingId());
        }
        return resolved != ChiseTweaksSettingsController.Surface.INSPECTOR;
    }

    static boolean isActionInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition.Action action) {
        if (action == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.HIGHLIGHT
                : surface;
        if (resolved == ChiseTweaksSettingsController.Surface.HIGHLIGHT) {
            return action == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT;
        }
        if (resolved == ChiseTweaksSettingsController.Surface.INSPECTOR) {
            return action == ChiseTweaksSettingRowDefinition.Action.SELECT_PATTERN_REFERENCE
                    || action == ChiseTweaksSettingRowDefinition.Action.CLEAR_PATTERN_REFERENCE;
        }
        return resolved == ChiseTweaksSettingsController.Surface.FILTER
                && (action == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER
                || action == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER);
    }

    private static boolean isReleasedHighlightRow(SettingRowId id) {
        if (id == null) return false;
        return SettingRowIds.HIGHLIGHT_FEATURES.contains(id)
                || SettingRowIds.HIGHLIGHT_DETAIL_INTERACTIVE.contains(id)
                || id.startsWith(MATERIAL_PREFIX)
                || id.startsWith(TECHNICAL_PREFIX)
                || id.startsWith(HIDDEN_PREFIX);
    }
}
