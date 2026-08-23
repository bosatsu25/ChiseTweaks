package dev.chise.chisetweaks.gui;

import java.util.Set;

/** UI availability判定で共有するrow IDを集約し、文字列の重複定義を避ける。 */
final class SettingRowIds {
    static final SettingRowId MATERIALS = SettingRowId.of("materials");
    static final SettingRowId NETHER = SettingRowId.of("nether");
    static final SettingRowId THREAD = SettingRowId.of("thread");
    static final SettingRowId HIDDEN = SettingRowId.of("hidden");
    static final SettingRowId KELP = SettingRowId.of("kelp");
    static final SettingRowId GLASS = SettingRowId.of("glass");
    static final SettingRowId FOCUS_BLOCKS = SettingRowId.of("focusBlocks");
    static final SettingRowId FOCUS_ENTITIES = SettingRowId.of("focusEntities");
    static final SettingRowId LAVA = SettingRowId.of("lava");
    static final SettingRowId ANCIENT_DEBRIS_ANALYZER = SettingRowId.of("ancientDebrisAnalyzer");
    static final SettingRowId FIRE_VISIBILITY = SettingRowId.of("fireVisibility");
    static final SettingRowId CHEST_VISIBILITY = SettingRowId.of("chestVisibility");
    static final SettingRowId WHITE_CONCRETE_VISIBILITY = SettingRowId.of("whiteConcreteVisibility");
    static final SettingRowId DIAGNOSTIC_RELOAD_STATE = SettingRowId.of("diagnosticReloadState");
    static final SettingRowId DIAGNOSTIC_COPY = SettingRowId.of("copyDiagnostics");
    static final SettingRowId DIAGNOSTIC_EXPORT = SettingRowId.of("exportDiagnostics");

    static final Set<SettingRowId> MAIN_INTERACTIVE = Set.of(
            MATERIALS, NETHER, THREAD, HIDDEN, KELP, GLASS,
            FOCUS_BLOCKS, FOCUS_ENTITIES, LAVA, ANCIENT_DEBRIS_ANALYZER,
            FIRE_VISIBILITY, CHEST_VISIBILITY, WHITE_CONCRETE_VISIBILITY);

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
            SettingRowId.of("fineThreadOpacity"),
            SettingRowId.of("hiddenSurfaceColor"),
            SettingRowId.of("hiddenSurfaceOpacity"));

    private SettingRowIds() {}
}
