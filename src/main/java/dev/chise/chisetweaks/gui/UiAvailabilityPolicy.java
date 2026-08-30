package dev.chise.chisetweaks.gui;

import java.util.Set;

/** 現行リリースのUI操作可否をfeature runtimeの利用可否から分離して判定する。 */
final class UiAvailabilityPolicy {
    private static final String MATERIAL_PREFIX = "visualTargetMaterial";
    private static final String TECHNICAL_PREFIX = "visualTargetTechnical";
    private static final String HIDDEN_PREFIX = "visualTargetHidden";
    private static final Set<String> BUILDER_HIGHLIGHT_FEATURES = Set.of(
            "materials", "nether", "kelp", "glass", "lava", "hidden");
    private static final Set<String> BUILDER_HIGHLIGHT_DETAILS = Set.of(
            "oreMotion", "moddedOreTargets",
            "lavaRange", "lavaVerticalRange", "lavaInterval", "lavaMaxOverlays",
            "hiddenRange", "hiddenVerticalRange", "hiddenInterval", "hiddenMaxOverlays",
            "hiddenSurfaceColor", "hiddenSurfaceOpacity");

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
            return isBuilderHighlightRow(row.id());
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
        if (resolved == ChiseTweaksSettingsController.Surface.FILTER) {
            return action == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER
                    || action == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER;
        }
        return resolved == ChiseTweaksSettingsController.Surface.INTEGRATIONS
                && (action == ChiseTweaksSettingRowDefinition.Action.EDIT_LITEMATICA_PICK_REDIRECT
                || action == ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKERMORE_AUTO_PICK_GUARD
                || action == ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKEROO_TOOL_SWITCH_GUARD
                || action == ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE);
    }

    private static boolean isBuilderHighlightRow(String id) {
        if (id == null) return false;
        return BUILDER_HIGHLIGHT_FEATURES.contains(id)
                || BUILDER_HIGHLIGHT_DETAILS.contains(id)
                || id.startsWith(MATERIAL_PREFIX)
                || id.startsWith(HIDDEN_PREFIX);
    }
}
