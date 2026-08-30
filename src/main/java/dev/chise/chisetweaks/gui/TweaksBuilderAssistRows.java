package dev.chise.chisetweaks.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Removes developer-oriented Inspector diagnostics while retaining the builder-facing assist tools. */
final class TweaksBuilderAssistRows {
    private static final Set<String> DEVELOPER_DIAGNOSTIC_ROWS = Set.of(
            "inspector.filter",
            "inspector.matchedRule",
            "inspector.features",
            "inspector.renderMode");

    private TweaksBuilderAssistRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        ArrayList<ChiseTweaksSettingRowDefinition> result = new ArrayList<>();
        result.add(ChiseTweaksSettingRowDefinition.header(
                "product.builderAssist",
                TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.INSPECTOR)));

        for (ChiseTweaksSettingRowDefinition row : InspectorSettingsRows.rows(snapshot, includeHelp)) {
            if ("inspector.title".equals(row.id())) continue;
            if (DEVELOPER_DIAGNOSTIC_ROWS.contains(row.id())) continue;
            result.add(row);
        }
        return List.copyOf(result);
    }
}
