package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.action;
import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.boolLiteral;
import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.header;
import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.headerLiteral;
import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.info;
import static dev.chise.chisetweaks.gui.TweaksProductSettingsRows.text;

/** Dynamic Inspector presentation: target, placement, schematic, pattern and history rows. */
final class InspectorSettingsRows {
    private static final String[] PROPERTY_GROUPS = {
            "orientation", "shape", "connection", "interaction", "fluid", "other"};

    private InspectorSettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        addRows(rows, snapshot, includeHelp);
        return List.copyOf(rows);
    }

    static void addRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        CrosshairInspector.Snapshot resolved = snapshot == null
                ? CrosshairInspector.Snapshot.noTarget()
                : snapshot;
        header(rows, "inspector.title", "screen.chisetweaks.inspector.title");
        boolLiteral(rows, "interactionHistory",
                LocalFeatureSettings.INTERACTION_HISTORY,
                text("screen.chisetweaks.inspector.interaction_history.name"),
                text("screen.chisetweaks.inspector.interaction_history.description"));
        boolLiteral(rows, "schematicPlacementInspector",
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR,
                text("screen.chisetweaks.inspector.schematic_inspector.name"),
                text("screen.chisetweaks.inspector.schematic_inspector.description"));
        if (resolved.targetKind() == HitResult.Type.MISS) {
            info(rows, "inspector.noTarget",
                    text("screen.chisetweaks.inspector.no_target"),
                    text("screen.chisetweaks.inspector.no_target.description"));
        } else {
            String targetLabel = resolved.targetKind() == HitResult.Type.BLOCK
                    ? text("screen.chisetweaks.inspector.target.block")
                    : text("screen.chisetweaks.inspector.target.entity");
            info(rows, "inspector.target", targetLabel, resolved.targetId());
            if (resolved.targetKind() == HitResult.Type.BLOCK) {
                addSemanticStateRows(rows, resolved.stateProperties());
            }
        }
        addPlacementRows(rows, resolved);
        addSchematicPlacementRows(rows);
        addPatternConsistencyRows(rows);
        addInteractionHistoryRows(rows);
        if (includeHelp) addCommonHelpRows(rows);
    }

    private static void addSchematicPlacementRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "schematicPlacement.title",
                text("screen.chisetweaks.inspector.schematic.title"));
        PlacementInspector.SchematicSnapshot schematic = PlacementInspector.schematicSnapshot();
        if (!schematic.available()) {
            info(rows, "schematicPlacement.none",
                    text("screen.chisetweaks.inspector.schematic.none"),
                    text("screen.chisetweaks.inspector.schematic.none.description"));
            return;
        }
        info(rows, "schematicPlacement.expected",
                text("screen.chisetweaks.inspector.schematic.expected"),
                schematic.expectedId());
        info(rows, "schematicPlacement.predicted",
                text("screen.chisetweaks.inspector.schematic.predicted"),
                schematic.predictedId());
        info(rows, "schematicPlacement.result",
                text("screen.chisetweaks.inspector.schematic.result"),
                schematic.resultLabel());
    }

    private static void addInteractionHistoryRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "interactionHistory.title",
                text("screen.chisetweaks.inspector.interaction_history.title"));
        List<InteractionHistory.Entry> history = InteractionHistory.snapshot();
        if (history.isEmpty()) {
            info(rows, "interactionHistory.empty",
                    text("screen.chisetweaks.inspector.interaction_history.empty"),
                    text("screen.chisetweaks.inspector.interaction_history.empty.description"));
            return;
        }
        int shown = Math.min(5, history.size());
        for (int index = 0; index < shown; index++) {
            InteractionHistory.Entry entry = history.get(index);
            info(rows,
                    "interactionHistory." + entry.sequence(),
                    entry.type().name().replace('_', ' '),
                    entry.summary());
        }
    }

    private static void addPatternConsistencyRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        PatternConsistencyInspector pattern = PatternConsistencyInspector.activeInspector();
        header(rows, "pattern.title", "screen.chisetweaks.pattern.title");
        action(rows, "pattern.select",
                text("screen.chisetweaks.pattern.select.name"),
                text("screen.chisetweaks.pattern.select.description"),
                ChiseTweaksSettingRowDefinition.Action.SELECT_PATTERN_REFERENCE,
                text("screen.chisetweaks.pattern.select.action"));
        if (pattern == null || !pattern.hasReference()) {
            info(rows, "pattern.inactive",
                    text("screen.chisetweaks.inspector.none"),
                    text("screen.chisetweaks.pattern.inactive.description"));
            return;
        }
        info(rows, "pattern.reference",
                text("screen.chisetweaks.pattern.reference"),
                pattern.referenceId() + "\n" + semanticProperties(pattern.referenceProperties()));
        info(rows, "pattern.status",
                text("screen.chisetweaks.pattern.status"),
                Component.translatable(
                        "screen.chisetweaks.pattern.status.value",
                        pattern.compared(), pattern.matches(), pattern.mismatchTotal()).getString());
        for (String group : PROPERTY_GROUPS) {
            String description = pattern.mismatchSummary(group);
            if (description.isEmpty()) continue;
            info(rows, "pattern.mismatch." + group,
                    text("screen.chisetweaks.inspector.state." + group),
                    description);
        }
        action(rows, "pattern.clear",
                text("screen.chisetweaks.pattern.clear.action"),
                "",
                ChiseTweaksSettingRowDefinition.Action.CLEAR_PATTERN_REFERENCE,
                text("screen.chisetweaks.pattern.clear.action"));
    }

    private static void addPlacementRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            CrosshairInspector.Snapshot placement) {
        header(rows, "placement.title", "screen.chisetweaks.placement.title");
        boolean comparison = placement.placementResult() != PlacementInspector.NONE;
        if (!comparison && placement.clickedFace() == null) {
            info(rows, "placement.none",
                    text("screen.chisetweaks.inspector.none"),
                    "");
            return;
        }
        if (placement.predictedPlacement() == null) {
            info(rows, "placement.impossible",
                    text("screen.chisetweaks.placement.impossible"),
                    placementReason(placement));
            return;
        }
        BlockState state = placement.predictedPlacement();
        info(rows, "placement.predicted",
                text("screen.chisetweaks.placement.predicted"),
                semanticProperties(PlacementInspector.placementStateProperties(state)));
        if (!comparison) {
            info(rows, "placement.reason",
                    text("screen.chisetweaks.placement.reason"),
                    placementReason(placement));
        } else if (placement.actualPlacement() == null) {
            info(rows, "placement.actual",
                    text("screen.chisetweaks.placement.actual"),
                    text("screen.chisetweaks.placement.awaiting_actual"));
        } else {
            info(rows, "placement.actual",
                    text("screen.chisetweaks.placement.actual"),
                    semanticProperties(PlacementInspector.actualPlacementStateProperties(
                            placement.actualPlacement())));
            info(rows, "placement.result",
                    text("screen.chisetweaks.placement.result"),
                    text(comparisonResultKey(placement.placementResult())));
            if (placement.placementResult() == PlacementInspector.ADJUSTED) {
                info(rows, "placement.changed",
                        text("screen.chisetweaks.placement.changed"),
                        changedPlacementProperties(state, placement.actualPlacement()));
            }
        }
    }

    private static String placementReason(CrosshairInspector.Snapshot placement) {
        return Component.translatable(placement.upperClick()
                        ? "screen.chisetweaks.placement.reason.upper"
                        : "screen.chisetweaks.placement.reason.lower",
                placement.clickedFace().getName()).getString();
    }

    private static void addSemanticStateRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            List<String> properties) {
        if (properties.isEmpty()) {
            info(rows, "inspector.blockState",
                    text("screen.chisetweaks.inspector.block_state"),
                    text("screen.chisetweaks.inspector.none"));
            return;
        }
        for (String group : PROPERTY_GROUPS) {
            String description = semanticProperties(properties, group);
            if (!description.isEmpty()) {
                info(rows, "inspector.state." + group,
                        text("screen.chisetweaks.inspector.state." + group),
                        description);
            }
        }
    }

    private static String semanticProperties(List<String> properties) {
        return semanticProperties(properties, null);
    }

    private static String semanticProperties(List<String> properties, String requiredGroup) {
        StringBuilder result = new StringBuilder();
        for (String property : properties) {
            int separator = property.indexOf('=');
            String name = property.substring(0, separator);
            if (requiredGroup != null && !requiredGroup.equals(CrosshairSnapshotPolicy.semanticPropertyGroup(name))) continue;
            if (!result.isEmpty()) result.append('\n');
            result.append(CrosshairSnapshotPolicy.humanize(name))
                    .append("  ")
                    .append(CrosshairSnapshotPolicy.humanize(property.substring(separator + 1)));
        }
        return result.toString();
    }

    private static String changedPlacementProperties(BlockState predicted, BlockState actual) {
        List<String> before = PlacementInspector.placementStateProperties(predicted);
        List<String> after = PlacementInspector.actualPlacementStateProperties(actual);
        StringBuilder changed = new StringBuilder();
        for (String property : before) {
            int separator = property.indexOf('=');
            String name = property.substring(0, separator);
            String actualProperty = findProperty(after, name);
            if (actualProperty == null || property.equals(actualProperty)) continue;
            if (!changed.isEmpty()) changed.append('\n');
            changed.append(CrosshairSnapshotPolicy.humanize(name))
                    .append(": ")
                    .append(CrosshairSnapshotPolicy.humanize(property.substring(separator + 1)))
                    .append(" → ")
                    .append(CrosshairSnapshotPolicy.humanize(actualProperty.substring(actualProperty.indexOf('=') + 1)));
        }
        return changed.toString();
    }

    private static String findProperty(List<String> properties, String name) {
        String prefix = name + "=";
        for (String property : properties) {
            if (property.startsWith(prefix)) return property;
        }
        return null;
    }

    static String comparisonResultKey(int result) {
        return switch (result) {
            case PlacementInspector.MATCH -> "screen.chisetweaks.placement.result.match";
            case PlacementInspector.ADJUSTED -> "screen.chisetweaks.placement.result.adjusted";
            case PlacementInspector.DIFFERENT -> "screen.chisetweaks.placement.result.different";
            default -> "screen.chisetweaks.placement.result.unavailable";
        };
    }

    private static void addCommonHelpRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "help.title", "screen.chisetweaks.help.title");
        for (String section : List.of(
                "highlight", "filter", "analyzer", "visibility", "settings", "troubleshooting")) {
            info(rows, "help." + section,
                    text("screen.chisetweaks.help." + section + ".name"),
                    text("screen.chisetweaks.help." + section + ".description"));
        }
    }


}
