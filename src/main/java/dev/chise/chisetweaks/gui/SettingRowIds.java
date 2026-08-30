package dev.chise.chisetweaks.gui;

import java.util.Set;

/** UI availability判定で共有するrow IDを集約し、文字列の重複定義を避ける。 */
final class SettingRowIds {
    static final SettingRowId MATERIALS = SettingRowId.of("materials");
    static final SettingRowId NETHER = SettingRowId.of("nether");
    static final SettingRowId THREAD = SettingRowId.of("thread");
    static final SettingRowId KELP = SettingRowId.of("kelp");
    static final SettingRowId GLASS = SettingRowId.of("glass");

    static final Set<SettingRowId> HIGHLIGHT_FEATURES = Set.of(
            MATERIALS, NETHER, THREAD, KELP, GLASS);

    static final Set<SettingRowId> HIGHLIGHT_DETAIL_INTERACTIVE = Set.of(
            SettingRowId.of("oreMotion"),
            SettingRowId.of("moddedOreTargets"),
            SettingRowId.of("highlightRange"),
            SettingRowId.of("highlightVerticalRange"),
            SettingRowId.of("highlightInterval"),
            SettingRowId.of("highlightMaxOverlays"),
            SettingRowId.of("highlightWorldOverlay"),
            SettingRowId.of("highlightDimensionPresets"),
            SettingRowId.of("fineThreadColor"),
            SettingRowId.of("fineThreadOpacity"));

    private SettingRowIds() {}
}
