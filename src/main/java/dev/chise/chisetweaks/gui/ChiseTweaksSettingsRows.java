package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;
import dev.chise.chisetweaks.config.CompatibilityIntegrationSettings;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.gui.ChiseTweaksSettingsController.Surface;
import dev.chise.chisetweaks.integration.masa.MasaModAvailability;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Pure presentation model for the six settings surfaces. */
final class ChiseTweaksSettingsRows {
    private ChiseTweaksSettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows() {
        return rows(Surface.HIGHLIGHT);
    }

    static List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        Surface resolved = surface == null
                ? Surface.HIGHLIGHT
                : surface;
        if (resolved == Surface.HIGHLIGHT) {
            addHighlightRows(rows);
        } else if (resolved == Surface.FILTER) {
            addFilterRows(rows);
        } else if (resolved == Surface.INSPECTOR) {
            InspectorSettingsRows.addRows(rows, CrosshairInspector.Snapshot.noTarget(), false);
        } else if (resolved == Surface.ANALYZER) {
            addAnalyzerRows(rows);
        } else if (resolved == Surface.VISIBILITY) {
            addVisibilityRows(rows);
        } else {
            addIntegrationRows(rows);
            addCompatibilityRows(rows);
        }
        return List.copyOf(rows);
    }

    static List<ChiseTweaksSettingRowDefinition> inspectorRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        return InspectorSettingsRows.rows(snapshot, includeHelp);
    }

    static String surfaceTitle(Surface surface) {
        String value = (surface == null ? Surface.HIGHLIGHT : surface)
                .name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void addHighlightRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.highlight", "Highlight");
        feature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS);
        feature(rows, "nether", FeatureSwitches.NETHER_PALETTE);
        feature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE);
        feature(rows, "glass", FeatureSwitches.GLASS_INSPECTION);
        feature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT);

        header(rows, "detail.highlight.general", "screen.chisetweaks.settings.section.shared");
        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "screen.chisetweaks.settings.ore_motion.name",
                "screen.chisetweaks.settings.ore_motion.description");
        action(rows, "moddedOreTargets",
                "screen.chisetweaks.settings.modded_ore.name",
                "screen.chisetweaks.settings.modded_ore.description",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);
        configInteger(rows, "highlightRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS, 1);
        configInteger(rows, "highlightVerticalRange", LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS, 1);
        configInteger(rows, "highlightInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL, 5);
        configInteger(rows, "highlightMaxOverlays", LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS, 1);
        configBool(rows, "highlightWorldOverlay", LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY);
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
        addTargets(rows, "visualTargetTechnical");

        header(rows, "detail.highlight.materialTargets", "screen.chisetweaks.settings.section.material_targets");
        addTargets(rows, "visualTargetMaterial");

    }

    private static void addFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.filter", "Filter");
        feature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS);
        feature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES);

        headerLiteral(rows, "detail.visualFilter.behavior", "Filter Settings");
        configBool(rows, "refreshRenderer", BuilderFocusConfig.REFRESH_RENDERER);
        action(rows, "editBlockFilter",
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.definition().englishName(),
                text("config.comment.builderfocusblocks"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER,
                text("screen.chisetweaks.settings.action.settings"));
        action(rows, "editEntityFilter",
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.definition().englishName(),
                text("config.comment.builderfocusentities"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER,
                text("screen.chisetweaks.settings.action.settings"));
    }

    private static void addAnalyzerRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.analyzer", "Analyzer");
        featureLiteral(rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                text("config.comment.locallavahighlight"));
        featureLiteral(rows, "villagerAnalyzer", FeatureSwitches.VILLAGER_ANALYZER,
                "Nearby villagers are linked to their claimed job site. If client JOB_SITE memory is unavailable, a bounded loaded-world workstation fallback is used.");
        featureLiteral(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                text("config.comment.hiddensurfacetrace"));

        analyzerBudgetRows(
                rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS);

        analyzerBudgetRows(
                rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
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
        addTargets(rows, "visualTargetHidden");
    }

    private static void analyzerBudgetRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String prefix,
            FeatureSwitch feature,
            ChiseIntegerSetting horizontal,
            ChiseIntegerSetting vertical,
            ChiseIntegerSetting interval,
            ChiseIntegerSetting maxOverlays) {
        headerLiteral(rows, "detail.analyzer." + prefix, feature.definition().englishName() + " Settings");
        String key = "screen.chisetweaks.settings." + prefix;
        integer(rows, prefix + "Range", horizontal, key + "_range.name", key + "_range.description", 1);
        integer(rows, prefix + "VerticalRange", vertical, key + "_vertical.name", key + "_vertical.description", 1);
        integer(rows, prefix + "Interval", interval, key + "_interval.name", key + "_interval.description", 5);
        integer(rows, prefix + "MaxOverlays", maxOverlays, key + "_max.name", key + "_max.description", 1);
    }

    private static void addVisibilityRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.visibility", "Visibility");
        featureLiteral(rows, "fireVisibility", FeatureSwitches.FIRE_VISIBILITY,
                "Lower only the first-person fire overlay.");
        integer(rows, "fireVisibilitySize", LocalFeatureSettings.FIRE_VISIBILITY_SIZE,
                "screen.chisetweaks.settings.fire_size.name",
                "screen.chisetweaks.settings.fire_size.description", 1);
        featureLiteral(rows, "chestVisibility", FeatureSwitches.BRIGHT_CHEST,
                "Improve Chest and Double Chest visibility.");
        featureLiteral(rows, "whiteConcreteVisibility", FeatureSwitches.BRIGHT_CONCRETE,
                "Improve White Concrete visibility.");
        featureLiteral(rows, "beaconRange", FeatureSwitches.BEACON_RANGE,
                "Show the horizontal effect radius of nearby active Beacons.");
        featureLiteral(rows, "lightningRodRange", FeatureSwitches.LIGHTNING_ROD_RANGE,
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


    static void header(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String translationKey) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, text(translationKey)));
    }

    static void headerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String name) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, name));
    }

    static void info(ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id, String name, String description) {
        rows.add(ChiseTweaksSettingRowDefinition.info(id, name, description));
    }

    private static void feature(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            FeatureSwitch config) {
        FeatureDefinition definition = config.definition();
        String descriptionKey = definition.nameKey().replace("config.name.", "config.comment.");
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id, definition.englishName(), text(descriptionKey), config));
    }

    private static void featureLiteral(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            FeatureSwitch config,
            String description) {
        boolLiteral(rows, id, config, config.definition().englishName(), description);
    }

    private static void configBool(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseBooleanSetting config) {
        String key = configOptionKey(config.getName());
        bool(rows, id, config, key + ".name", key + ".comment");
    }

    private static void configInteger(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseIntegerSetting config,
            int step) {
        String key = configOptionKey(config.getName());
        integer(rows, id, config, key + ".name", key + ".comment", step);
    }

    private static String configOptionKey(String name) {
        return "config.option." + name.toLowerCase(java.util.Locale.ROOT);
    }

    private static void bool(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseBooleanSetting config, String nameKey, String descriptionKey) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, text(nameKey), text(descriptionKey), config));
    }

    static void boolLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
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

    static void action(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            String nameKey, String descriptionKey, ChiseTweaksSettingRowDefinition.Action action) {
        action(rows, id, text(nameKey), text(descriptionKey), action,
                text("screen.chisetweaks.settings.action.settings"));
    }

    static void action(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            String name, String description, ChiseTweaksSettingRowDefinition.Action action, String actionLabel) {
        rows.add(ChiseTweaksSettingRowDefinition.action(id, name, description, action, actionLabel));
    }

    private static void target(ArrayList<ChiseTweaksSettingRowDefinition> rows, ChiseBooleanSetting config) {
        String base = "screen.chisetweaks.settings.target." + config.getName();
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(), text(base + ".name"), text(base + ".description"), config));
    }

    private static void addTargets(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String prefix) {
        for (ChiseBooleanSetting option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName().startsWith(prefix)) target(rows, option);
        }
    }

    static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static void addCompatibilityRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        boolean nvidiumInstalled = FabricLoader.getInstance().isModLoaded("nvidium");
        rows.add(ChiseTweaksSettingRowDefinition.header(
                                "compatibility.renderer.header", "Renderer Compatibility"));
        rows.add(ChiseTweaksSettingRowDefinition.info(
                                "compatibility.nvidium.status",
                                "Nvidium",
                                nvidiumInstalled ? "Installed / 導入済み" : "Not installed / 未導入 - World Border Fixはno-opです"));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                                "worldBorderFixEnabled",
                                "World Border Fix",
                                "ワールドボーダー付近・遠距離座標でNvidium描画が破綻する環境向け。危険領域だけNvidiumを一時停止します。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_ENABLED));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                                "worldBorderFixXray",
                                "World Border X-Ray Guard",
                                "ワールドボーダー接近時の透明化/X-Ray状描画を抑制します。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_XRAY));
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                                "worldBorderFixDistance",
                                "Border Trigger Distance",
                                "この距離よりワールドボーダーへ近づいたとき抑制候補にします。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_DISTANCE,
                                16));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                                "worldBorderFixFarCoords",
                                "Far Coordinate Guard",
                                "大きなX/Z座標でのNvidium描画破綻を抑制します。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_FAR_COORDS));
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                                "worldBorderFixCoordThreshold",
                                "Far Coordinate Threshold",
                                "|X|または|Z|がこの値以上で抑制候補にします。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_COORD_THRESHOLD,
                                1000));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                                "worldBorderFixAutoReenable",
                                "Immediate Nvidium Re-enable",
                                "危険領域を離れた直後に描画再読込してNvidiumを復帰します。既定OFF。安定性優先ならOFFを推奨します。",
                                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_AUTO_REENABLE));
    }



}
