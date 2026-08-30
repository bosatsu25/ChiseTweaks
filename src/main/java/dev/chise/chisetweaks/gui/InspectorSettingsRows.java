package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.action;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.boolLiteral;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.header;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.headerLiteral;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.info;
import static dev.chise.chisetweaks.gui.ChiseTweaksSettingsRows.text;

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
                "Interaction History",
                "直近64件までの配置・破壊・Item使用・Entity操作をメモリ内だけに保持します。チャット、看板、本、Container内容は記録しません。");
        boolLiteral(rows, "schematicPlacementInspector",
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR,
                "Schematic Placement Inspector",
                "Litematicaの設計図と現在の配置予測をMATCH / COMPATIBLE / DIFFERENTで比較します。配置自体は止めません。");
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
            info(rows, "inspector.filter",
                    text("screen.chisetweaks.inspector.filter"),
                    text(resolved.filterDecision().hidden()
                            ? "screen.chisetweaks.inspector.filter.hidden"
                            : "screen.chisetweaks.inspector.filter.visible"));
            info(rows, "inspector.matchedRule",
                    text("screen.chisetweaks.inspector.matched_rule"),
                    filterReason(resolved.filterDecision()));
            info(rows, "inspector.features",
                    text("screen.chisetweaks.inspector.responsible_feature"),
                    resolved.responsibleFeatures().isEmpty()
                            ? text("screen.chisetweaks.inspector.none")
                            : joinFeatures(resolved.responsibleFeatures(), false, false));
            info(rows, "inspector.renderMode",
                    text("screen.chisetweaks.inspector.render_mode"),
                    resolved.responsibleFeatures().isEmpty()
                            ? text("screen.chisetweaks.inspector.none")
                            : joinFeatures(
                                    resolved.responsibleFeatures(),
                                    true,
                                    resolved.filterDecision().hidden()));
        }
        addPlacementRows(rows, resolved);
        addSchematicPlacementRows(rows);
        addPatternConsistencyRows(rows);
        addInteractionHistoryRows(rows);
        if (includeHelp) addCommonHelpRows(rows);
    }

    private static void addSchematicPlacementRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "schematicPlacement.title", "Schematic Placement");
        PlacementInspector.SchematicSnapshot schematic = PlacementInspector.schematicSnapshot();
        if (!schematic.available()) {
            info(rows, "schematicPlacement.none",
                    "No active schematic comparison",
                    "Litematica + Schematic Placement Inspectorが有効で、設計図上へBlockを配置すると比較結果を表示します。");
            return;
        }
        info(rows, "schematicPlacement.expected",
                "Expected / 設計図",
                schematic.expectedId());
        info(rows, "schematicPlacement.predicted",
                "Predicted / 配置予測",
                schematic.predictedId());
        info(rows, "schematicPlacement.result",
                "Result",
                schematic.resultLabel());
    }

    private static void addInteractionHistoryRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "interactionHistory.title", "Interaction History");
        List<InteractionHistory.Entry> history = InteractionHistory.snapshot();
        if (history.isEmpty()) {
            info(rows, "interactionHistory.empty",
                    "No history",
                    "Interaction Historyが有効になると、直近の読み取り専用interactionをここに表示します。");
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
                semanticProperties(CrosshairInspector.placementStateProperties(state)));
        if (!comparison) {
            info(rows, "placement.reason",
                    text("screen.chisetweaks.inspector.matched_rule"),
                    placementReason(placement));
        } else if (placement.actualPlacement() == null) {
            info(rows, "placement.actual",
                    text("screen.chisetweaks.placement.actual"),
                    text("screen.chisetweaks.placement.awaiting_actual"));
        } else {
            info(rows, "placement.actual",
                    text("screen.chisetweaks.placement.actual"),
                    semanticProperties(CrosshairInspector.actualPlacementStateProperties(
                            placement.actualPlacement())));
            info(rows, "placement.result",
                    text("screen.chisetweaks.placement.result"),
                    text(CrosshairInspector.comparisonResultKey(placement.placementResult())));
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
            if (requiredGroup != null && !requiredGroup.equals(CrosshairInspector.semanticPropertyGroup(name))) continue;
            if (!result.isEmpty()) result.append('\n');
            result.append(CrosshairInspector.humanize(name))
                    .append("  ")
                    .append(CrosshairInspector.humanize(property.substring(separator + 1)));
        }
        return result.toString();
    }

    private static String changedPlacementProperties(BlockState predicted, BlockState actual) {
        List<String> before = CrosshairInspector.placementStateProperties(predicted);
        List<String> after = CrosshairInspector.actualPlacementStateProperties(actual);
        StringBuilder changed = new StringBuilder();
        for (String property : before) {
            int separator = property.indexOf('=');
            String name = property.substring(0, separator);
            String actualProperty = findProperty(after, name);
            if (actualProperty == null || property.equals(actualProperty)) continue;
            if (!changed.isEmpty()) changed.append('\n');
            changed.append(CrosshairInspector.humanize(name))
                    .append(": ")
                    .append(CrosshairInspector.humanize(property.substring(separator + 1)))
                    .append(" → ")
                    .append(CrosshairInspector.humanize(actualProperty.substring(actualProperty.indexOf('=') + 1)));
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

    private static String filterReason(BuilderFocusVisibility.FilterDecision decision) {
        String reason = text("screen.chisetweaks.inspector.reason." + decision.reason());
        return decision.matchedRule().isEmpty() ? reason : reason + ": " + decision.matchedRule();
    }

    private static String joinFeatures(
            List<FeatureDefinition> features,
            boolean modes,
            boolean hidden) {
        StringBuilder result = new StringBuilder();
        for (FeatureDefinition feature : features) {
            if (!result.isEmpty()) result.append('\n');
            result.append(modes ? text(CrosshairInspector.renderModeKey(feature, hidden)) : feature.englishName());
        }
        return result.toString();
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
