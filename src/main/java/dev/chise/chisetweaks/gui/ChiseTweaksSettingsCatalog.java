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
        switch (surface == null ? ChiseTweaksSettingsController.Surface.MAIN : surface) {
            case MAIN -> addMainRows(rows);
            case HIGHLIGHT_DETAILS -> addHighlightDetailRows(rows);
            case VISUAL_FILTER_DETAILS -> addVisualFilterDetailRows(rows);
            case LAVA_DETAILS -> addAnalyzerDetailRows(rows);
        }
        return List.copyOf(rows);
    }

    String surfaceTitle(ChiseTweaksSettingsController.Surface surface) {
        return switch (surface == null ? ChiseTweaksSettingsController.Surface.MAIN : surface) {
            case MAIN -> "";
            case HIGHLIGHT_DETAILS -> text("screen.chisetweaks.settings.title.highlight");
            case VISUAL_FILTER_DETAILS -> text("screen.chisetweaks.settings.section.visual_filter");
            case LAVA_DETAILS -> text("screen.chisetweaks.settings.title.analyzer");
        };
    }

    private static void addMainRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        rows.add(ChiseTweaksSettingRowDefinition.headerAction(
                "header.highlight",
                "Highlight",
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS,
                text("screen.chisetweaks.settings.action.settings")));
        compactFeature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.MATERIAL_HIGHLIGHTS);
        compactFeature(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE);
        compactFeature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE);
        compactFeature(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE);
        compactFeature(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                FeatureDefinition.GLASS_INSPECTION);
        compactFeature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT,
                FeatureDefinition.KELP_HIGHLIGHT);

        rows.add(ChiseTweaksSettingRowDefinition.headerAction(
                "header.visualFilter",
                "Visual Filter",
                ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS,
                text("screen.chisetweaks.settings.action.settings")));
        compactFeature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS);
        compactFeature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES);

        rows.add(ChiseTweaksSettingRowDefinition.headerAction(
                "header.analyzer",
                "Analyzer",
                ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS,
                text("screen.chisetweaks.settings.action.settings")));
        boolLiteral(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "Lava Analyzer", "Analyze nearby lava source blocks in already-loaded chunks");
        boolLiteral(rows, "ancientDebrisAnalyzer", LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER,
                "Ancient Debris Analyzer", "Analyze Ancient Debris in already-loaded Nether chunks");

        headerLiteral(rows, "header.visibilityImprovement", "Visibility");
        boolLiteral(rows, "fireVisibility", LocalFeatureSwitches.FIRE_VISIBILITY,
                "Low Fire", "Lower the first-person fire overlay");
        boolLiteral(rows, "chestVisibility", ChestVisibilitySetting.INSTANCE,
                "Bright Chest", "Use the bright high-visibility chest texture");
        boolLiteral(rows, "whiteConcreteVisibility", WhiteConcreteVisibilitySetting.INSTANCE,
                "Bright Concrete", "Use the high-visibility white concrete texture");
    }

    private static void addHighlightDetailRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
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
                "Dimension preset / ディメンションプリセット",
                "Auto-switch to a smaller, higher-contrast Nether profile / ネザーでは範囲を少し抑えた高コントラスト表示へ自動切替");

        headerLiteral(rows, "detail.highlight.traceAppearance", "Trace appearance / 表示スタイル");
        integerLiteral(rows, "fineThreadColor",
                LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                text(FeatureDefinition.FINE_THREAD_TRACE.nameKey()) + " - Color / 色",
                "AUTO keeps the current Chise palette / AUTOは現在のChise配色を維持", 1);
        integerLiteral(rows, "fineThreadOpacity",
                LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                text(FeatureDefinition.FINE_THREAD_TRACE.nameKey()) + " - Opacity / 不透明度",
                "20-100%", 5);
        integerLiteral(rows, "hiddenSurfaceColor",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                text(FeatureDefinition.HIDDEN_SURFACE_TRACE.nameKey()) + " - Color / 色",
                "AUTO keeps per-target colors / AUTOは対象別の既定色を維持", 1);
        integerLiteral(rows, "hiddenSurfaceOpacity",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                text(FeatureDefinition.HIDDEN_SURFACE_TRACE.nameKey()) + " - Opacity / 不透明度",
                "20-100%", 5);

        headerLiteral(rows, "detail.highlight.technicalTargets", "Fine Thread targets / 細線対象");
        for (ChiseBooleanSetting option : TECHNICAL_TARGETS) targetLiteral(rows, option);

        header(rows, "detail.highlight.materialTargets", "screen.chisetweaks.settings.section.material_targets");
        for (ChiseBooleanSetting option : RESOURCE_TARGETS) target(rows, option);

        header(rows, "detail.highlight.hiddenTargets", "screen.chisetweaks.settings.section.hidden_targets");
        for (ChiseBooleanSetting option : VISIBILITY_TARGETS) target(rows, option);
    }

    private static void addVisualFilterDetailRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "detail.visualFilter.behavior", "Visual Filter / 表示フィルター");
        bool(rows, "refreshRenderer", BuilderFocusConfig.REFRESH_RENDERER,
                "config.option.refreshbuilderfocusrenderer.name",
                "config.option.refreshbuilderfocusrenderer.comment");
        action(rows, "editBlockFilter",
                FeatureDefinition.BUILDER_FOCUS_BLOCKS.nameKey(),
                "config.comment.builderfocusblocks",
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER);
        action(rows, "editEntityFilter",
                FeatureDefinition.BUILDER_FOCUS_ENTITIES.nameKey(),
                "config.comment.builderfocusentities",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER);
    }

    private static void addAnalyzerDetailRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "detail.analyzer.lava", "Lava Analyzer");
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

        headerLiteral(rows, "detail.analyzer.ancientDebris", "Ancient Debris Analyzer");
        integer(rows, "ancientDebrisRange", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                "screen.chisetweaks.settings.debris_range.name",
                "screen.chisetweaks.settings.debris_range.description", 16);
        integer(rows, "ancientDebrisMaxMarkers", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                "screen.chisetweaks.settings.debris_max.name",
                "screen.chisetweaks.settings.debris_max.description", 8);
    }

    private static void header(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String translationKey) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, text(translationKey)));
    }

    private static void headerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String name) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, name));
    }

    private static void compactFeature(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
            ChiseBooleanSetting config, FeatureDefinition definition) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, definition.englishName(), "", config));
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
        rows.add(ChiseTweaksSettingRowDefinition.action(
                id, text(nameKey), text(descriptionKey), action,
                text("screen.chisetweaks.settings.action.settings")));
    }

    private static void target(ArrayList<ChiseTweaksSettingRowDefinition> rows, ChiseBooleanSetting config) {
        String base = "screen.chisetweaks.settings.target." + config.getName();
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(), text(base + ".name"), text(base + ".description"), config));
    }

    private static void targetLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, ChiseBooleanSetting config) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(),
                config.getDisplayName(false) + " / " + config.getDisplayName(true),
                config.getComment(false) + " / " + config.getComment(true),
                config));
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
