package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import dev.chise.chisetweaks.integration.masa.MasaModAvailability;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** 設定画面で使うlocalize済みimmutable row定義を構築する。 */
final class ChiseTweaksSettingsCatalog {
    private static final String[] PROPERTY_GROUPS = {
            "orientation", "shape", "connection", "interaction", "fluid", "other"};
    private static final List<ChiseBooleanSetting> RESOURCE_TARGETS = targets("visualTargetMaterial");
    private static final List<ChiseBooleanSetting> TECHNICAL_TARGETS = targets("visualTargetTechnical");
    private static final List<ChiseBooleanSetting> VISIBILITY_TARGETS = targets("visualTargetHidden");

    List<ChiseTweaksSettingRowDefinition> rows(ChiseTweaksSettingsController.Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        ChiseTweaksSettingsController.Surface resolved = surface == null
                ? ChiseTweaksSettingsController.Surface.HIGHLIGHT
                : surface;
        if (resolved == ChiseTweaksSettingsController.Surface.HIGHLIGHT) {
            addHighlightRows(rows);
        } else if (resolved == ChiseTweaksSettingsController.Surface.FILTER) {
            addFilterRows(rows);
        } else if (resolved == ChiseTweaksSettingsController.Surface.INSPECTOR) {
            addInspectorRows(rows, CrosshairInspector.Snapshot.noTarget(), false);
        } else if (resolved == ChiseTweaksSettingsController.Surface.ANALYZER) {
            addAnalyzerRows(rows);
        } else if (resolved == ChiseTweaksSettingsController.Surface.VISIBILITY) {
            addVisibilityRows(rows);
        } else {
            addIntegrationRows(rows);
        }
        return List.copyOf(rows);
    }

    List<ChiseTweaksSettingRowDefinition> inspectorRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        addInspectorRows(rows, snapshot, includeHelp);
        return List.copyOf(rows);
    }

    String surfaceTitle(ChiseTweaksSettingsController.Surface surface) {
        if (surface == ChiseTweaksSettingsController.Surface.FILTER) return "Filter";
        if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) return "Inspector";
        if (surface == ChiseTweaksSettingsController.Surface.ANALYZER) return "Analyzer";
        if (surface == ChiseTweaksSettingsController.Surface.VISIBILITY) return "Visibility";
        return surface == ChiseTweaksSettingsController.Surface.INTEGRATIONS
                ? "Integrations"
                : "Highlight";
    }

    private static void addHighlightRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.highlight", "Highlight");
        feature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.MATERIAL_HIGHLIGHTS);
        feature(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE);
        feature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE);
        feature(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                FeatureDefinition.GLASS_INSPECTION);
        feature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT,
                FeatureDefinition.KELP_HIGHLIGHT);

        header(rows, "detail.highlight.general", "screen.chisetweaks.settings.section.shared");
        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "screen.chisetweaks.settings.ore_motion.name",
                "screen.chisetweaks.settings.ore_motion.description");
        action(rows, "moddedOreTargets",
                "screen.chisetweaks.settings.modded_ore.name",
                "screen.chisetweaks.settings.modded_ore.description",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);
        integer(rows, "highlightRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "config.option.localworksitevisibilityhorizontalradius.name",
                "config.option.localworksitevisibilityhorizontalradius.comment", 1);
        integer(rows, "highlightVerticalRange", LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS,
                "config.option.localworksitevisibilityverticalradius.name",
                "config.option.localworksitevisibilityverticalradius.comment", 1);
        integer(rows, "highlightInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "config.option.localworksitevisibilityintervalticks.name",
                "config.option.localworksitevisibilityintervalticks.comment", 5);
        integer(rows, "highlightMaxOverlays", LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS,
                "config.option.localworksitevisibilitymaxoverlays.name",
                "config.option.localworksitevisibilitymaxoverlays.comment", 1);
        bool(rows, "highlightWorldOverlay", LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY,
                "config.option.localworksitevisibilityworldoverlay.name",
                "config.option.localworksitevisibilityworldoverlay.comment");
        boolLiteral(rows, "highlightDimensionPresets",
                LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                "Dimension Preset",
                "Automatically use the bounded Nether visibility profile when appropriate.");

        headerLiteral(rows, "detail.highlight.traceAppearance", "Trace Appearance");
        integerLiteral(rows, "fineThreadColor",
                LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                "Fine Line Highlight - Color",
                "AUTO keeps the current Chise palette.", 1);
        integerLiteral(rows, "fineThreadOpacity",
                LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                "Fine Line Highlight - Opacity",
                "20-100%", 5);
        headerLiteral(rows, "detail.highlight.technicalTargets", "Fine Line Targets");
        for (ChiseBooleanSetting option : TECHNICAL_TARGETS) target(rows, option);

        header(rows, "detail.highlight.materialTargets", "screen.chisetweaks.settings.section.material_targets");
        for (ChiseBooleanSetting option : RESOURCE_TARGETS) target(rows, option);

    }

    private static void addFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.filter", "Filter");
        feature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS);
        feature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES);

        headerLiteral(rows, "detail.visualFilter.behavior", "Filter Settings");
        bool(rows, "refreshRenderer", BuilderFocusConfig.REFRESH_RENDERER,
                "config.option.refreshbuilderfocusrenderer.name",
                "config.option.refreshbuilderfocusrenderer.comment");
        action(rows, "editBlockFilter",
                FeatureDefinition.BUILDER_FOCUS_BLOCKS.englishName(),
                text("config.comment.builderfocusblocks"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER,
                text("screen.chisetweaks.settings.action.settings"));
        action(rows, "editEntityFilter",
                FeatureDefinition.BUILDER_FOCUS_ENTITIES.englishName(),
                text("config.comment.builderfocusentities"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER,
                text("screen.chisetweaks.settings.action.settings"));
    }

    private static void addAnalyzerRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.analyzer", "Analyzer");
        boolLiteral(rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                FeatureDefinition.LAVA_HIGHLIGHT.englishName(),
                text("config.comment.locallavahighlight"));
        boolLiteral(rows, "villagerAnalyzer", FeatureSwitches.VILLAGER_ANALYZER,
                FeatureDefinition.VILLAGER_ANALYZER.englishName(),
                "Nearby villagers are linked to their claimed job site. If client JOB_SITE memory is unavailable, a bounded loaded-world workstation fallback is used.");
        boolLiteral(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE.englishName(),
                text("config.comment.hiddensurfacetrace"));

        analyzerBudgetRows(
                rows, "lava", FeatureDefinition.LAVA_HIGHLIGHT,
                LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS);

        analyzerBudgetRows(
                rows, "hidden", FeatureDefinition.HIDDEN_SURFACE_TRACE,
                LocalFeatureSettings.HIDDEN_ANALYZER_HORIZONTAL_RADIUS,
                LocalFeatureSettings.HIDDEN_ANALYZER_VERTICAL_RADIUS,
                LocalFeatureSettings.HIDDEN_ANALYZER_INTERVAL,
                LocalFeatureSettings.HIDDEN_ANALYZER_MAX_OVERLAYS);
        integerLiteral(rows, "hiddenSurfaceColor",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                "Hidden Block Analyzer - Color",
                "AUTO keeps the Chise hidden-block palette.", 1);
        integerLiteral(rows, "hiddenSurfaceOpacity",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                "Hidden Block Analyzer - Opacity",
                "20-100%", 5);

        header(rows, "detail.analyzer.hiddenTargets", "screen.chisetweaks.settings.section.hidden_targets");
        for (ChiseBooleanSetting option : VISIBILITY_TARGETS) target(rows, option);
    }

    private static void analyzerBudgetRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String prefix,
            FeatureDefinition definition,
            ChiseIntegerSetting horizontal,
            ChiseIntegerSetting vertical,
            ChiseIntegerSetting interval,
            ChiseIntegerSetting maxOverlays) {
        headerLiteral(rows, "detail.analyzer." + prefix, definition.englishName() + " Settings");
        String key = "screen.chisetweaks.settings." + prefix;
        integer(rows, prefix + "Range", horizontal, key + "_range.name", key + "_range.description", 1);
        integer(rows, prefix + "VerticalRange", vertical, key + "_vertical.name", key + "_vertical.description", 1);
        integer(rows, prefix + "Interval", interval, key + "_interval.name", key + "_interval.description", 5);
        integer(rows, prefix + "MaxOverlays", maxOverlays, key + "_max.name", key + "_max.description", 1);
    }

    private static void addVisibilityRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.visibility", "Visibility");
        boolLiteral(rows, "fireVisibility", FeatureSwitches.FIRE_VISIBILITY,
                FeatureDefinition.FIRE_VISIBILITY.englishName(),
                "Lower only the first-person fire overlay.");
        integer(rows, "fireVisibilitySize", LocalFeatureSettings.FIRE_VISIBILITY_SIZE,
                "screen.chisetweaks.settings.fire_size.name",
                "screen.chisetweaks.settings.fire_size.description", 1);
        boolLiteral(rows, "chestVisibility", FeatureSwitches.BRIGHT_CHEST,
                FeatureDefinition.BRIGHT_CHEST.englishName(), "Improve Chest and Double Chest visibility.");
        boolLiteral(rows, "whiteConcreteVisibility", FeatureSwitches.BRIGHT_CONCRETE,
                FeatureDefinition.BRIGHT_CONCRETE.englishName(), "Improve White Concrete visibility.");
        boolLiteral(rows, "beaconRange", FeatureSwitches.BEACON_RANGE,
                FeatureDefinition.BEACON_RANGE.englishName(),
                "Show the horizontal effect radius of nearby active Beacons.");
        boolLiteral(rows, "lightningRodRange", FeatureSwitches.LIGHTNING_ROD_RANGE,
                FeatureDefinition.LIGHTNING_ROD_RANGE.englishName(),
                "Show the Vanilla 128-block horizontal Lightning Rod attraction range.");
    }

    private static void addIntegrationRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        MasaModAvailability.Snapshot installed = MasaModAvailability.snapshot();
        headerLiteral(rows, "header.integrations", "Masa Ecosystem");
        info(rows, "masa.summary",
                "Masa Integration",
                "Masa系MODの既存機能を補助・制御するoptional integrationです。ChiseTweaks自身は自動操作を実行しません。");
        integerLiteral(rows, "masaJapaneseUiMode",
                MasaIntegrationSettings.JAPANESE_UI_MODE,
                "Masa Japanese UI",
                "AutoはMinecraftが日本語のときだけ日本語UX補助を有効にします。", 1);
        action(rows, "openMasaGuide",
                "Masa Guide",
                "Masa系MODが何を担当し、どの設定画面を見るべきかを日本語で案内します。",
                ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE,
                "ガイドを開く");

        headerLiteral(rows, "masa.installed", "Installed Mods");
        info(rows, "masa.malilib", "MaLiLib", installedLabel(installed.malilib()));
        info(rows, "masa.litematica", "Litematica", installedLabel(installed.litematica()));
        info(rows, "masa.tweakeroo", "Tweakeroo", installedLabel(installed.tweakeroo()));
        info(rows, "masa.tweakermore", "TweakerMore", installedLabel(installed.tweakermore()));
        info(rows, "masa.syncmatica", "Syncmatica", installedLabel(installed.syncmatica()));

        headerLiteral(rows, "masa.litematica.settings", "Litematica");
        boolLiteral(rows, "litematicaPickRedirect",
                MasaIntegrationSettings.LITEMATICA_PICK_REDIRECT,
                "Pick Redirect",
                "Litematicaが要求するblock itemが無い場合に、設定済みの代替blockを候補にします。");
        action(rows, "editLitematicaPickRedirect",
                "Pick Redirect Map",
                "Schematic Block → Replacement Blockの対応を編集します。",
                ChiseTweaksSettingRowDefinition.Action.EDIT_LITEMATICA_PICK_REDIRECT,
                "リスト設定");

        headerLiteral(rows, "masa.tweakeroo.settings", "Tweakeroo");
        boolLiteral(rows, "tweakerooToolSwitchGuard",
                MasaIntegrationSettings.TWEAKEROO_TOOL_SWITCH_GUARD,
                "Selective Tool Switch Guard",
                "TweakerooのTool SwitchをChiseのallow/deny policyで制御します。");
        action(rows, "editTweakerooToolSwitchGuard",
                "Tool Switch Guard List",
                "Tool Switchを許可/拒否するBlock IDを編集します。",
                ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKEROO_TOOL_SWITCH_GUARD,
                "リスト設定");
        boolLiteral(rows, "tweakerooPersistentGammaOverride",
                MasaIntegrationSettings.TWEAKEROO_PERSISTENT_GAMMA,
                "Persistent Gamma Override",
                "Tweakerooが所有するGamma Override状態の復元だけを補助します。");

        headerLiteral(rows, "masa.tweakermore.settings", "TweakerMore");
        boolLiteral(rows, "tweakermoreAutoPickGuard",
                MasaIntegrationSettings.TWEAKERMORE_AUTO_PICK_GUARD,
                "Selective Auto Pick Guard",
                "TweakerMoreのAuto Pickをitem allow/deny policyで制御します。");
        action(rows, "editTweakerMoreAutoPickGuard",
                "Auto Pick Guard List",
                "Auto Pickを許可/拒否するItem IDを編集します。",
                ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKERMORE_AUTO_PICK_GUARD,
                "リスト設定");
        boolLiteral(rows, "tweakermoreMaterialListRefresh",
                MasaIntegrationSettings.TWEAKERMORE_MATERIAL_REFRESH,
                "Material List Refresh",
                "TweakerMoreのMaterial collect完了後に既存Material List表示を同期します。");

        headerLiteral(rows, "masa.syncmatica.settings", "Syncmatica");
        boolLiteral(rows, "syncmaticaRemoveDisabled",
                MasaIntegrationSettings.SYNCMATICA_REMOVE_DISABLED,
                "Disable Remove",
                "共有Schematicの削除操作をguardします。");
        boolLiteral(rows, "syncmaticaRemoveRequireShift",
                MasaIntegrationSettings.SYNCMATICA_REQUIRE_SHIFT,
                "Require Shift To Remove",
                "共有Schematic削除時にShift押下を要求します。");
    }

    private static String installedLabel(boolean installed) {
        return installed ? "Installed / 導入済み" : "Not installed / 未導入";
    }

    private static void addInspectorRows(
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
        SchematicPlacementInspector.Snapshot schematic = SchematicPlacementInspector.snapshot();
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
        boolean comparison = placement.placementResult() != PlacementComparisonTracker.NONE;
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
                    text(comparisonResultKey(placement.placementResult())));
            if (placement.placementResult() == PlacementComparisonTracker.ADJUSTED) {
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

    static String semanticPropertyGroup(String property) {
        return switch (property == null ? "" : property) {
            case "facing", "axis" -> "orientation";
            case "half", "type", "shape", "face" -> "shape";
            case "north", "south", "east", "west", "up", "down", "in_wall" -> "connection";
            case "open", "powered", "lit", "honey_level" -> "interaction";
            case "waterlogged" -> "fluid";
            default -> "other";
        };
    }

    static String comparisonResultKey(int result) {
        return switch (result) {
            case PlacementComparisonTracker.MATCH -> "screen.chisetweaks.placement.result.match";
            case PlacementComparisonTracker.ADJUSTED -> "screen.chisetweaks.placement.result.adjusted";
            case PlacementComparisonTracker.DIFFERENT -> "screen.chisetweaks.placement.result.different";
            default -> "screen.chisetweaks.placement.result.unavailable";
        };
    }

    private static String semanticProperties(List<String> properties) {
        return semanticProperties(properties, null);
    }

    private static String semanticProperties(List<String> properties, String requiredGroup) {
        StringBuilder result = new StringBuilder();
        for (String property : properties) {
            int separator = property.indexOf('=');
            String name = property.substring(0, separator);
            if (requiredGroup != null && !requiredGroup.equals(semanticPropertyGroup(name))) continue;
            if (!result.isEmpty()) result.append('\n');
            result.append(humanize(name))
                    .append("  ")
                    .append(humanize(property.substring(separator + 1)));
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
            changed.append(humanize(name))
                    .append(": ")
                    .append(humanize(property.substring(separator + 1)))
                    .append(" → ")
                    .append(humanize(actualProperty.substring(actualProperty.indexOf('=') + 1)));
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

    static String humanize(String token) {
        String value = token.replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
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
            result.append(modes ? text(renderModeKey(feature, hidden)) : feature.englishName());
        }
        return result.toString();
    }

    static String renderModeKey(FeatureDefinition feature, boolean hidden) {
        if (hidden) return "screen.chisetweaks.inspector.render_mode.suppressed";
        return feature == FeatureDefinition.LAVA_HIGHLIGHT
                || feature == FeatureDefinition.HIDDEN_SURFACE_TRACE
                ? "screen.chisetweaks.inspector.render_mode.through_wall"
                : "screen.chisetweaks.inspector.render_mode.visible";
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

    private static void header(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String translationKey) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, text(translationKey)));
    }

    private static void headerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String name) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, name));
    }

    private static void info(ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id, String name, String description) {
        rows.add(ChiseTweaksSettingRowDefinition.info(id, name, description));
    }

    private static void feature(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseBooleanSetting config, FeatureDefinition definition) {
        String descriptionKey = definition.nameKey().replace("config.name.", "config.comment.");
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id, definition.englishName(), text(descriptionKey), config));
    }

    private static void bool(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseBooleanSetting config, String nameKey, String descriptionKey) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, text(nameKey), text(descriptionKey), config));
    }

    private static void boolLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseBooleanSetting config, String name, String description) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, name, description, config));
    }

    private static void integer(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseIntegerSetting config, String nameKey, String descriptionKey, int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(id, text(nameKey), text(descriptionKey), config, step));
    }

    private static void integerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseIntegerSetting config, String name, String description, int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(id, name, description, config, step));
    }

    private static void action(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            String nameKey, String descriptionKey, ChiseTweaksSettingRowDefinition.Action action) {
        action(rows, id, text(nameKey), text(descriptionKey), action,
                text("screen.chisetweaks.settings.action.settings"));
    }

    private static void action(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            String name, String description, ChiseTweaksSettingRowDefinition.Action action, String actionLabel) {
        rows.add(ChiseTweaksSettingRowDefinition.action(id, name, description, action, actionLabel));
    }

    private static void target(ArrayList<ChiseTweaksSettingRowDefinition> rows, ChiseBooleanSetting config) {
        String base = "screen.chisetweaks.settings.target." + config.getName();
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(), text(base + ".name"), text(base + ".description"), config));
    }

    private static List<ChiseBooleanSetting> targets(String prefix) {
        ArrayList<ChiseBooleanSetting> result = new ArrayList<>();
        for (ChiseBooleanSetting option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName().startsWith(prefix)) result.add(option);
        }
        return List.copyOf(result);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
