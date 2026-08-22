package dev.chise.chisetweaks.gui;

/** Keeps unreleased controls visible but non-interactive during the current visual-feature pre-release. */
final class PreReleaseUiPolicy {
    private PreReleaseUiPolicy() {}

    static boolean isHighlightBulkInteractive() {
        return false;
    }

    static boolean isRowInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition row) {
        if (row == null) return false;
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.MAIN
                : surface;

        if (row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
            return row.action() == null || (resolved == ChiseTweaksSettingsController.Surface.MAIN
                    && ("header.highlight".equals(row.id())
                    || "header.visibilityImprovement".equals(row.id())));
        }

        return switch (resolved) {
            case MAIN -> "materials".equals(row.id())
                    || "kelp".equals(row.id())
                    || "glass".equals(row.id())
                    || "lava".equals(row.id())
                    || "ancientDebrisAnalyzer".equals(row.id());
            case HIGHLIGHT_DETAILS -> isOreHighlightDetail(row.id());
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
            case LAVA_DETAILS -> false;
        };
    }

    private static boolean isOreHighlightDetail(String id) {
        if (id == null) return false;
        return "oreMotion".equals(id)
                || "moddedOreTargets".equals(id)
                || id.startsWith("visualTargetMaterial");
    }
}
