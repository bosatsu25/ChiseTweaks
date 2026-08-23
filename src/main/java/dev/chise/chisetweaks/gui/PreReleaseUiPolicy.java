package dev.chise.chisetweaks.gui;

/** @deprecated Use {@link UiAvailabilityPolicy}. */
@Deprecated(forRemoval = true)
final class PreReleaseUiPolicy {
    private PreReleaseUiPolicy() {}

    static boolean isRowInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition row) {
        return UiAvailabilityPolicy.isRowInteractive(surface, row);
    }

    static boolean isActionInteractive(
            ChiseTweaksSettingsController.Surface surface,
            ChiseTweaksSettingRowDefinition.Action action) {
        return UiAvailabilityPolicy.isActionInteractive(surface, action);
    }
}
