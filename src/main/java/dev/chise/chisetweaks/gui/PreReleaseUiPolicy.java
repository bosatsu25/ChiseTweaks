package dev.chise.chisetweaks.gui;

/** @deprecated 正式なUI判定APIとして{@link UiAvailabilityPolicy}を使用する。 */
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
