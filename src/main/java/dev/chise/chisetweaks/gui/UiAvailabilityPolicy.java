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
                ? ChiseTweaksSettingsController.Surface.MAIN
                : surface;
        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) return true;
        return switch (resolved) {
            case MAIN -> SettingRowIds.MAIN_INTERACTIVE.contains(row.settingId());
            case HIGHLIGHT_DETAILS -> isReleasedHighlightDetail(row.settingId());
            case VISUAL_FILTER_DETAILS, LAVA_DETAILS -> true;
        };
    }

    static boolean isActionInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition.Action action) {
        if (action == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.MAIN
                : surface;
        return switch (resolved) {
            case MAIN -> action == ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS
                    || action == ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS
                    || action == ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS
                    || action == ChiseTweaksSettingRowDefinition.Action.COPY_DIAGNOSTICS
                    || action == ChiseTweaksSettingRowDefinition.Action.EXPORT_DIAGNOSTICS;
            case HIGHLIGHT_DETAILS -> action == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT;
            case VISUAL_FILTER_DETAILS -> action == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER
                    || action == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER;
            case LAVA_DETAILS -> false;
        };
    }

    private static boolean isReleasedHighlightDetail(SettingRowId id) {
        if (id == null) return false;
        return SettingRowIds.HIGHLIGHT_DETAIL_INTERACTIVE.contains(id)
                || id.startsWith(MATERIAL_PREFIX)
                || id.startsWith(TECHNICAL_PREFIX)
                || id.startsWith(HIDDEN_PREFIX);
    }
}
