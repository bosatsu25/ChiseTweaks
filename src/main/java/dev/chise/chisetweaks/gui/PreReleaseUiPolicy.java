package dev.chise.chisetweaks.gui;

final class PreReleaseUiPolicy {
    private PreReleaseUiPolicy() {}

    static boolean isRowInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition row) {
        if (row == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.MAIN
                : surface;

        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) return true;

        return switch (resolved) {
            case MAIN -> "materials".equals(row.id())
                    || "thread".equals(row.id())
                    || "hidden".equals(row.id())
                    || "kelp".equals(row.id())
                    || "glass".equals(row.id())
                    || "lava".equals(row.id())
                    || "ancientDebrisAnalyzer".equals(row.id());
            case HIGHLIGHT_DETAILS -> isReleasedHighlightDetail(row.id());
            case VISUAL_FILTER_DETAILS -> false;
            case LAVA_DETAILS -> true;
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
                    || action == ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS;
            case HIGHLIGHT_DETAILS -> action == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT;
            case VISUAL_FILTER_DETAILS, LAVA_DETAILS -> false;
        };
    }

    private static boolean isReleasedHighlightDetail(String id) {
        if (id == null) return false;
        return "oreMotion".equals(id)
                || "moddedOreTargets".equals(id)
                || "highlightRange".equals(id)
                || "highlightVerticalRange".equals(id)
                || "highlightInterval".equals(id)
                || "highlightMaxOverlays".equals(id)
                || "highlightWorldOverlay".equals(id)
                || "highlightExclusiveMode".equals(id)
                || "highlightDimensionPresets".equals(id)
                || "fineThreadColor".equals(id)
                || "fineThreadOpacity".equals(id)
                || "hiddenSurfaceColor".equals(id)
                || "hiddenSurfaceOpacity".equals(id)
                || id.startsWith("visualTargetMaterial")
                || id.startsWith("visualTargetTechnical")
                || id.startsWith("visualTargetHidden");
    }
}
