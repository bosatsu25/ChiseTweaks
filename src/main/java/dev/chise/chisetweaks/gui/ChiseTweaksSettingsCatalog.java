package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChestVisibilitySetting;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.config.WhiteConcreteVisibilitySetting;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 設定画面で使うlocalize済みimmutable row定義を構築する。 */
final class ChiseTweaksSettingsCatalog {
    private static final List<ChiseBooleanSetting> RESOURCE_TARGETS = targets("visualTargetMaterial");
    private static final List<ChiseBooleanSetting> TECHNICAL_TARGETS = targets("visualTargetTechnical");
    private static final List<ChiseBooleanSetting> VISIBILITY_TARGETS = targets("visualTargetHidden");

    List<ChiseTweaksSettingRowDefinition> rows(ChiseTweaksSettingsController.Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        switch (surface == null ? ChiseTweaksSettingsController.Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> addHighlightRows(rows);
            case VISUAL_FILTER -> addVisualFilterRows(rows);
            case ANALYZER -> addAnalyzerRows(rows);
            case VISIBILITY -> addVisibilityRows(rows);
            case HELP -> addHelpRows(rows);
        }
        return List.copyOf(rows);
    }

    String surfaceTitle(ChiseTweaksSettingsController.Surface surface) {
        return switch (surface == null ? ChiseTweaksSettingsController.Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> "Highlight";
            case VISUAL_FILTER -> "Visual Filter";
            case ANALYZER -> "Analyzer";
            case VISIBILITY -> "Visibility";
            case HELP -> "使い方";
        };
    }

    private static void addHighlightRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.highlight", "Highlight");
        feature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.MATERIAL_HIGHLIGHTS, "config.comment.materialhighlights");
        feature(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE, "config.comment.netherpalette");
        feature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE, "config.comment.finethreadtrace");
        feature(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE, "config.comment.hiddensurfacetrace");
        feature(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                FeatureDefinition.GLASS_INSPECTION, "config.comment.glassinspection");
        feature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT,
                FeatureDefinition.KELP_HIGHLIGHT, "config.comment.kelphighlight");

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
        integerLiteral(rows, "hiddenSurfaceColor",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                "Hidden Block Highlight - Color",
                "AUTO keeps per-target colors.", 1);
        integerLiteral(rows, "hiddenSurfaceOpacity",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                "Hidden Block Highlight - Opacity",
                "20-100%", 5);

        headerLiteral(rows, "detail.highlight.technicalTargets", "Fine Line Targets");
        for (ChiseBooleanSetting option : TECHNICAL_TARGETS) target(rows, option);

        header(rows, "detail.highlight.materialTargets", "screen.chisetweaks.settings.section.material_targets");
        for (ChiseBooleanSetting option : RESOURCE_TARGETS) target(rows, option);

        header(rows, "detail.highlight.hiddenTargets", "screen.chisetweaks.settings.section.hidden_targets");
        for (ChiseBooleanSetting option : VISIBILITY_TARGETS) target(rows, option);
    }

    private static void addVisualFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.visualFilter", "Visual Filter");
        feature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS, "config.comment.builderfocusblocks");
        feature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES, "config.comment.builderfocusentities");

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
        boolLiteral(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                FeatureDefinition.LAVA_HIGHLIGHT.englishName(),
                text("config.comment.locallavahighlight"));
        boolLiteral(rows, "ancientDebrisAnalyzer", LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.englishName(),
                text("config.comment.localancientdebrisanalyzer"));

        headerLiteral(rows, "detail.analyzer.lava", "Lava Analyzer Settings");
        integer(rows, "lavaRange", LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                "screen.chisetweaks.settings.lava_range.name",
                "screen.chisetweaks.settings.lava_range.description", 1);
        integer(rows, "lavaVerticalRange", LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                "screen.chisetweaks.settings.lava_vertical.name",
                "screen.chisetweaks.settings.lava_vertical.description", 1);
        integer(rows, "lavaInterval", LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                "screen.chisetweaks.settings.lava_interval.name",
                "screen.chisetweaks.settings.lava_interval.description", 5);
        integer(rows, "lavaMaxOverlays", LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS,
                "screen.chisetweaks.settings.lava_max.name",
                "screen.chisetweaks.settings.lava_max.description", 1);

        headerLiteral(rows, "detail.analyzer.ancientDebris", "Ancient Debris Analyzer Settings");
        integer(rows, "ancientDebrisRange", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                "screen.chisetweaks.settings.debris_range.name",
                "screen.chisetweaks.settings.debris_range.description", 16);
        integer(rows, "ancientDebrisMaxMarkers", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                "screen.chisetweaks.settings.debris_max.name",
                "screen.chisetweaks.settings.debris_max.description", 8);
    }

    private static void addVisibilityRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.visibility", "Visibility");
        boolLiteral(rows, "fireVisibility", LocalFeatureSwitches.FIRE_VISIBILITY,
                FeatureDefinition.FIRE_VISIBILITY.englishName(),
                "Lower only the first-person fire overlay.");
        boolLiteral(rows, "chestVisibility", ChestVisibilitySetting.INSTANCE,
                "Bright Chest", "Improve Chest and Double Chest visibility.");
        boolLiteral(rows, "whiteConcreteVisibility", WhiteConcreteVisibilitySetting.INSTANCE,
                "Bright Concrete", "Improve White Concrete visibility.");
    }

    private static void addHelpRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "help.title", "ChiseTweaks の使い方");
        info(rows, "help.highlight", "Highlight",
                "鉱石・細線・隠れブロック・ガラス・昆布などを見つけやすくします。各Highlightは同時にONにできます。");
        info(rows, "help.visualFilter", "Visual Filter",
                "指定したBlock / Entityの描画をローカルだけで整理します。サーバー上の状態は変更しません。");
        info(rows, "help.analyzer", "Analyzer",
                "Lava SourceとAncient Debrisを読み込み済みチャンクだけから解析します。未ロードチャンクを強制ロードしません。");
        info(rows, "help.visibility", "Visibility",
                "Low Fireとbuilt-in Resource Packで、炎・Chest・White Concreteの見やすさを改善します。");
        info(rows, "help.settings", "設定",
                "変更があると下部ボタンは「設定を適用」に変わります。未変更時の「設定をリセット」は現在のタブを初期値へ戻します。");
        info(rows, "help.troubleshooting", "トラブルシューティング",
                "問題が起きた場合はPrism Launcherのコンソールまたはlogs/latest.logを確認してください。診断情報は設定画面へ重複表示しません。");
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
            ChiseBooleanSetting config, FeatureDefinition definition, String descriptionKey) {
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
