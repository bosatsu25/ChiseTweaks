package dev.chise.chisetweaks.gui;

import java.util.ArrayList;
import java.util.List;

/** Builder-facing assist composition without legacy Inspector diagnostics. */
final class TweaksBuilderAssistRows {
    private TweaksBuilderAssistRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        ArrayList<ChiseTweaksSettingRowDefinition> result = new ArrayList<>();
        result.add(ChiseTweaksSettingRowDefinition.header(
                "product.builderAssist",
                TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.INSPECTOR)));

        for (ChiseTweaksSettingRowDefinition row : InspectorSettingsRows.rows(snapshot, includeHelp)) {
            if (!"inspector.title".equals(row.id())) result.add(row);
        }
        return List.copyOf(result);
    }
}
