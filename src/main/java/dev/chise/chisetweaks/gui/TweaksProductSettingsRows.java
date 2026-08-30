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

/** Single presentation source for the Tweaks-oriented settings product. */
final class TweaksProductSettingsRows {
    private TweaksProductSettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        switch (surface == null ? Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> addBuilderHighlights(rows);
            case FILTER -> addSceneFilter(rows);
            case ANALYZER -> addTechnicalVisualization(rows);
            case VISIBILITY -> addVisualTweaks(rows);
            case INTEGRATIONS -> {
                addIntegrations(rows);
                addCompatibilityRows(rows);
            }
            case INSPECTOR -> {
                return List.of();
            }
        }
        return List.copyOf(rows);
    }

    static String title(Surface surface) {
        return switch (surface == null ? Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> text("screen.chisetweaks.product.builder_highlights");
            case FILTER -> text("screen.chisetweaks.product.scene_filter");
            case INSPECTOR -> text("screen.chisetweaks.product.builder_assist");
            case ANALYZER -> text("screen.chisetweaks.product.technical");
            case VISIBILITY -> text("screen.chisetweaks.product.visual_tweaks");
            case INTEGRATIONS -> text("screen.chisetweaks.product.integrations");
        };
    }

    private static void addBuilderHighlights(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "product.builderHighlights", title(Surface.HIGHLIGHT));
        feature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS);
        feature(rows, "nether", FeatureSwitches.NETHER_PALETTE);
        feature(rows, "glass", FeatureSwitches.GLASS_INSPECTION);
        feature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT);

        header(rows, "detail.highlight.material", "screen.chisetweaks.settings.section.shared");
        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "screen.chisetweaks.settings.ore_motion.name",
                "screen.chisetweaks.settings.ore_motion.description");
        action(rows, "moddedOreTargets",
                "screen.chisetweaks.settings.modded_ore.name",
                "screen.chisetweaks.settings.modded_ore.description",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);
        header(rows, "detail.highlight.materialTargets", "screen.chisetweaks.settings.section.material_targets");
        addTargets(rows, "visualTargetMaterial");

        featureLiteral(rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                text("screen.chisetweaks.product.lava.description"));
        analyzerBudgetRows(rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS);

        featureLiteral(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                text("screen.chisetweaks.product.hidden.description"));
        analyzerBudgetRows(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                LocalFeatureSettings.HIDDEN_ANALYZER_HORIZONTAL_RADIUS,
                LocalFeatureSettings.HIDDEN_ANALYZER_VERTICAL_RADIUS,
                LocalFeatureSettings.HIDDEN_ANALYZER_INTERVAL,
                LocalFeatureSettings.HIDDEN_ANALYZER_MAX_OVERLAYS);
        integerLiteral(rows, "hiddenSurfaceColor",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                text("screen.chisetweaks.settings.copy.hidden_color.name"),
                text("screen.chisetweaks.settings.copy.hidden_color.description"), 1);
        integerLiteral(rows, "hiddenSurfaceOpacity",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                text("screen.chisetweaks.settings.copy.hidden_opacity.name"),
                text("screen.chisetweaks.settings.copy.opacity_range"), 5);
        header(rows, "detail.highlight.hiddenTargets", "screen.chisetweaks.settings.section.hidden_targets");
        addTargets(rows, "visualTargetHidden");
    }

    private static void addSceneFilter(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "product.sceneFilter", title(Surface.FILTER));
        feature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS);
        feature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES);
        headerLiteral(rows, "detail.sceneFilter.behavior",
                text("screen.chisetweaks.settings.copy.filter_settings"));
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

    private static void addTechnicalVisualization(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "product.technical", title(Surface.ANALYZER));
        feature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE);

        header(rows, "detail.technical.general", "screen.chisetweaks.settings.section.shared");
        configInteger(rows, "highlightRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS, 1);
        configInteger(rows, "highlightVerticalRange", LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS, 1);
        configInteger(rows, "highlightInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL, 5);
        configInteger(rows, "highlightMaxOverlays", LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS, 1);
        configBool(rows, "highlightWorldOverlay", LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY);
        boolLiteral(rows, "highlightDimensionPresets",
                LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                text("screen.chisetweaks.settings.copy.dimension_preset.name"),
                text("screen.chisetweaks.settings.copy.dimension_preset.description"));

        headerLiteral(rows, "detail.technical.traceAppearance",
                text("screen.chisetweaks.settings.copy.trace_appearance"));
        integerLiteral(rows, "fineThreadColor",
                LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                text("screen.chisetweaks.settings.copy.fine_line_color.name"),
                text("screen.chisetweaks.settings.copy.fine_line_color.description"), 1);
        integerLiteral(rows, "fineThreadOpacity",
                LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                text("screen.chisetweaks.settings.copy.fine_line_opacity.name"),
                text("screen.chisetweaks.settings.copy.opacity_range"), 5);
        headerLiteral(rows, "detail.technical.targets",
                text("screen.chisetweaks.settings.copy.fine_line_targets"));
        addTargets(rows, "visualTargetTechnical");

        featureLiteral(rows, "beaconRange", FeatureSwitches.BEACON_RANGE,
                text("screen.chisetweaks.settings.copy.beacon_range.description"));
        featureLiteral(rows, "lightningRodRange", FeatureSwitches.LIGHTNING_ROD_RANGE,
                text("screen.chisetweaks.settings.copy.lightning_rod_range.description"));
        featureLiteral(rows, "villagerAnalyzer", FeatureSwitches.VILLAGER_ANALYZER,
                text("screen.chisetweaks.product.villager_links.description"));
    }

    private static void addVisualTweaks(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "product.visualTweaks", title(Surface.VISIBILITY));
        featureLiteral(rows, "fireVisibility", FeatureSwitches.FIRE_VISIBILITY,
                text("screen.chisetweaks.settings.copy.fire_visibility.description"));
        integer(rows, "fireVisibilitySize", LocalFeatureSettings.FIRE_VISIBILITY_SIZE,
                "screen.chisetweaks.settings.fire_size.name",
                "screen.chisetweaks.settings.fire_size.description", 1);
        featureLiteral(rows, "handheldSize", FeatureSwitches.HANDHELD_SIZE,
                text("screen.chisetweaks.settings.copy.handheld_size.description"));
        integer(rows, "handheldBlockScale", LocalFeatureSettings.HANDHELD_BLOCK_SCALE,
                "screen.chisetweaks.settings.handheld_block.name",
                "screen.chisetweaks.settings.handheld_block.description", 5);
        integer(rows, "handheldItemScale", LocalFeatureSettings.HANDHELD_ITEM_SCALE,
                "screen.chisetweaks.settings.handheld_item.name",
                "screen.chisetweaks.settings.handheld_item.description", 5);
        integer(rows, "handheldToolScale", LocalFeatureSettings.HANDHELD_TOOL_SCALE,
                "screen.chisetweaks.settings.handheld_tool.name",
                "screen.chisetweaks.settings.handheld_tool.description", 5);
        featureLiteral(rows, "chestVisibility", FeatureSwitches.BRIGHT_CHEST,
                text("screen.chisetweaks.settings.copy.bright_chest.description"));
        featureLiteral(rows, "whiteConcreteVisibility", FeatureSwitches.BRIGHT_CONCRETE,
                text("screen.chisetweaks.settings.copy.bright_concrete.description"));
    }

    private static void addIntegrations(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        MasaModAvailability.Snapshot installed = MasaModAvailability.snapshot();
        headerLiteral(rows, "product.integrations", title(Surface.INTEGRATIONS));
        info(rows, "masa.summary",
                text("screen.chisetweaks.settings.copy.masa_summary.name"),
                text("screen.chisetweaks.settings.copy.masa_summary.description"));
        integerLiteral(rows, "masaJapaneseUiMode",
                MasaIntegrationSettings.JAPANESE_UI_MODE,
                text("screen.chisetweaks.settings.copy.masa_japanese_ui.name"),
                text("screen.chisetweaks.settings.copy.masa_japanese_ui.description"), 1);
        action(rows, "openMasaGuide",
                text("screen.chisetweaks.settings.copy.masa_guide.name"),
                text("screen.chisetweaks.settings.copy.masa_guide.description"),
                ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE,
                text("screen.chisetweaks.settings.copy.masa_guide.action"));

        headerLiteral(rows, "masa.installed", text("screen.chisetweaks.settings.copy.installed_mods"));
        info(rows, "masa.malilib", "MaLiLib", installedLabel(installed.malilib()));
        info(rows, "masa.litematica", "Litematica", installedLabel(installed.litematica()));
        info(rows, "masa.tweakeroo", "Tweakeroo", installedLabel(installed.tweakeroo()));
        info(rows, "masa.tweakermore", "TweakerMore", installedLabel(installed.tweakermore()));
        info(rows, "masa.syncmatica", "Syncmatica", installedLabel(installed.syncmatica()));

        headerLiteral(rows, "masa.litematica.settings", "Litematica");
        boolLiteral(rows, "litematicaPickRedirect",
                MasaIntegrationSettings.LITEMATICA_PICK_REDIRECT,
                text("screen.chisetweaks.settings.copy.pick_redirect.name"),
                text("screen.chisetweaks.settings.copy.pick_redirect.description"));
        action(rows, "editLitematicaPickRedirect",
                text("screen.chisetweaks.settings.copy.pick_redirect_map.name"),
                text("screen.chisetweaks.settings.copy.pick_redirect_map.description"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_LITEMATICA_PICK_REDIRECT,
                text("screen.chisetweaks.settings.copy.list_settings"));

        headerLiteral(rows, "masa.tweakeroo.settings", "Tweakeroo");
        boolLiteral(rows, "tweakerooToolSwitchGuard",
                MasaIntegrationSettings.TWEAKEROO_TOOL_SWITCH_GUARD,
                text("screen.chisetweaks.settings.copy.tool_switch_guard.name"),
                text("screen.chisetweaks.settings.copy.tool_switch_guard.description"));
        action(rows, "editTweakerooToolSwitchGuard",
                text("screen.chisetweaks.settings.copy.tool_switch_list.name"),
                text("screen.chisetweaks.settings.copy.tool_switch_list.description"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKEROO_TOOL_SWITCH_GUARD,
                text("screen.chisetweaks.settings.copy.list_settings"));
        boolLiteral(rows, "tweakerooPersistentGammaOverride",
                MasaIntegrationSettings.TWEAKEROO_PERSISTENT_GAMMA,
                text("screen.chisetweaks.settings.copy.gamma_override.name"),
                text("screen.chisetweaks.settings.copy.gamma_override.description"));

        headerLiteral(rows, "masa.tweakermore.settings", "TweakerMore");
        boolLiteral(rows, "tweakermoreAutoPickGuard",
                MasaIntegrationSettings.TWEAKERMORE_AUTO_PICK_GUARD,
                text("screen.chisetweaks.settings.copy.auto_pick_guard.name"),
                text("screen.chisetweaks.settings.copy.auto_pick_guard.description"));
        action(rows, "editTweakerMoreAutoPickGuard",
                text("screen.chisetweaks.settings.copy.auto_pick_list.name"),
                text("screen.chisetweaks.settings.copy.auto_pick_list.description"),
                ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKERMORE_AUTO_PICK_GUARD,
                text("screen.chisetweaks.settings.copy.list_settings"));
        boolLiteral(rows, "tweakermoreMaterialListRefresh",
                MasaIntegrationSettings.TWEAKERMORE_MATERIAL_REFRESH,
                text("screen.chisetweaks.settings.copy.material_refresh.name"),
                text("screen.chisetweaks.settings.copy.material_refresh.description"));

        headerLiteral(rows, "masa.syncmatica.settings", "Syncmatica");
        boolLiteral(rows, "syncmaticaRemoveDisabled",
                MasaIntegrationSettings.SYNCMATICA_REMOVE_DISABLED,
                text("screen.chisetweaks.settings.copy.disable_remove.name"),
                text("screen.chisetweaks.settings.copy.disable_remove.description"));
        boolLiteral(rows, "syncmaticaRemoveRequireShift",
                MasaIntegrationSettings.SYNCMATICA_REQUIRE_SHIFT,
                text("screen.chisetweaks.settings.copy.require_shift.name"),
                text("screen.chisetweaks.settings.copy.require_shift.description"));
    }

    private static void addCompatibilityRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        boolean nvidiumInstalled = FabricLoader.getInstance().isModLoaded("nvidium");
        headerLiteral(rows, "compatibility.renderer.header",
                text("screen.chisetweaks.settings.copy.renderer_compatibility"));
        info(rows, "compatibility.nvidium.status", "Nvidium",
                text(nvidiumInstalled
                        ? "screen.chisetweaks.settings.copy.installed"
                        : "screen.chisetweaks.settings.copy.nvidium_not_installed"));
        boolLiteral(rows, "worldBorderFixEnabled",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_ENABLED,
                text("screen.chisetweaks.settings.copy.world_border_fix.name"),
                text("screen.chisetweaks.settings.copy.world_border_fix.description"));
        boolLiteral(rows, "worldBorderFixXray",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_XRAY,
                text("screen.chisetweaks.settings.copy.world_border_xray.name"),
                text("screen.chisetweaks.settings.copy.world_border_xray.description"));
        integerLiteral(rows, "worldBorderFixDistance",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_DISTANCE,
                text("screen.chisetweaks.settings.copy.border_distance.name"),
                text("screen.chisetweaks.settings.copy.border_distance.description"), 16);
        boolLiteral(rows, "worldBorderFixFarCoords",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_FAR_COORDS,
                text("screen.chisetweaks.settings.copy.far_coords.name"),
                text("screen.chisetweaks.settings.copy.far_coords.description"));
        integerLiteral(rows, "worldBorderFixCoordThreshold",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_COORD_THRESHOLD,
                text("screen.chisetweaks.settings.copy.coord_threshold.name"),
                text("screen.chisetweaks.settings.copy.coord_threshold.description"), 1000);
        boolLiteral(rows, "worldBorderFixAutoReenable",
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_AUTO_REENABLE,
                text("screen.chisetweaks.settings.copy.nvidium_reenable.name"),
                text("screen.chisetweaks.settings.copy.nvidium_reenable.description"));
    }

    private static void analyzerBudgetRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String prefix,
            FeatureSwitch feature,
            ChiseIntegerSetting horizontal,
            ChiseIntegerSetting vertical,
            ChiseIntegerSetting interval,
            ChiseIntegerSetting maxOverlays) {
        headerLiteral(rows, "detail.highlight." + prefix,
                Component.translatable(
                        "screen.chisetweaks.settings.copy.feature_settings",
                        feature.definition().englishName()).getString());
        String key = "screen.chisetweaks.settings." + prefix;
        integer(rows, prefix + "Range", horizontal, key + "_range.name", key + "_range.description", 1);
        integer(rows, prefix + "VerticalRange", vertical, key + "_vertical.name", key + "_vertical.description", 1);
        integer(rows, prefix + "Interval", interval, key + "_interval.name", key + "_interval.description", 5);
        integer(rows, prefix + "MaxOverlays", maxOverlays, key + "_max.name", key + "_max.description", 1);
    }

    static void header(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String translationKey) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, text(translationKey)));
    }

    static void headerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String name) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, name));
    }

    static void info(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id, String name, String description) {
        rows.add(ChiseTweaksSettingRowDefinition.info(id, name, description));
    }

    private static void feature(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            FeatureSwitch config) {
        FeatureDefinition definition = config.definition();
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id,
                definition.englishName(),
                text(definition.nameKey().replace("config.name.", "config.comment.")),
                config));
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

    private static void bool(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseBooleanSetting config,
            String nameKey,
            String descriptionKey) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, text(nameKey), text(descriptionKey), config));
    }

    static void boolLiteral(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseBooleanSetting config,
            String name,
            String description) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(id, name, description, config));
    }

    private static void integer(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseIntegerSetting config,
            String nameKey,
            String descriptionKey,
            int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                id, text(nameKey), text(descriptionKey), config, step));
    }

    private static void integerLiteral(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseIntegerSetting config,
            String name,
            String description,
            int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(id, name, description, config, step));
    }

    static void action(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            String nameKey,
            String descriptionKey,
            ChiseTweaksSettingRowDefinition.Action action) {
        action(rows, id, text(nameKey), text(descriptionKey), action,
                text("screen.chisetweaks.settings.action.settings"));
    }

    static void action(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            String name,
            String description,
            ChiseTweaksSettingRowDefinition.Action action,
            String actionLabel) {
        rows.add(ChiseTweaksSettingRowDefinition.action(id, name, description, action, actionLabel));
    }

    private static void addTargets(ArrayList<ChiseTweaksSettingRowDefinition> rows, String prefix) {
        for (ChiseBooleanSetting option : VisualTargetSettings.ALL_OPTIONS) {
            if (!option.getName().startsWith(prefix)) continue;
            String base = "screen.chisetweaks.settings.target." + option.getName();
            rows.add(ChiseTweaksSettingRowDefinition.bool(
                    option.getName(), text(base + ".name"), text(base + ".description"), option));
        }
    }

    static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static String installedLabel(boolean installed) {
        return text(installed
                ? "screen.chisetweaks.settings.copy.installed"
                : "screen.chisetweaks.settings.copy.not_installed");
    }

}
