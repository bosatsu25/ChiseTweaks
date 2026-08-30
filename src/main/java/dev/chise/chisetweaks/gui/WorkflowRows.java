package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureSettings;

import java.util.ArrayList;
import java.util.List;

import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.boolLiteral;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.headerLiteral;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.info;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.text;

/** Dynamic Workflow presentation for the bounded, memory-only interaction history. */
final class WorkflowRows {
    private WorkflowRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows() {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        headerLiteral(rows, "workflow.history",
                text("screen.chisetweaks.workflow.history.title"));
        boolLiteral(rows, "interactionHistory",
                LocalFeatureSettings.INTERACTION_HISTORY,
                text("screen.chisetweaks.inspector.interaction_history.name"),
                text("screen.chisetweaks.inspector.interaction_history.description"));

        List<InteractionHistory.Entry> history = InteractionHistory.snapshot();
        if (history.isEmpty()) {
            info(rows, "interactionHistory.empty",
                    text("screen.chisetweaks.inspector.interaction_history.empty"),
                    text("screen.chisetweaks.inspector.interaction_history.empty.description"));
            return List.copyOf(rows);
        }
        int shown = Math.min(5, history.size());
        for (int index = 0; index < shown; index++) {
            InteractionHistory.Entry entry = history.get(index);
            info(rows,
                    "interactionHistory." + entry.sequence(),
                    entry.type().name().replace('_', ' '),
                    entry.summary());
        }
        return List.copyOf(rows);
    }
}
