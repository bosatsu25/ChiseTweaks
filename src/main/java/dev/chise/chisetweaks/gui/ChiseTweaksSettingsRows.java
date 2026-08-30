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

/** Pure presentation model for the seven Tweaks product groups. */
final class ChiseTweaksSettingsRows {
    private ChiseTweaksSettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows() {
        return rows(Surface.VISUAL);
    }

    static List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        Surface resolved = surface == null ? Surface.VISUAL : surface;
        switch (resolved) {
            case VISUAL -> addVisualRows(rows);
            case BUILDER_HIGHLIGHTS -> addBuilderHighlightRows(rows);
            case TECHNICAL_VISUALIZATION -> addTechnicalVisualizationRows(rows);
            case SCENE_FILTER -> addSceneFilterRows(rows);
            case BUILDER_ASSIST -> BuilderAssistRows.addRows(
                    rows, CrosshairInspector.Snapshot.noTarget(), false);
            case WORKFLOW -> rows.addAll(WorkflowRows.rows());
            case INTEGRATIONS -> {
                addIntegrationRows(rows);
                addCompatibilityRows(rows);
            }
        }
        return List.copyOf(rows);
    }

    static String surfaceTitle(Surface surface) {
        Surface resolved = surface == null ? Surface.VISUAL : surface;
        return text("screen.chisetweaks.settings.tab."
                + resolved.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void addVisualRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "visual.firstPerson",
                text("screen.chisetweaks.settings.group.first_person"));
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

        headerLiteral(rows, "visual.brightBlocks",
                text("screen.chisetweaks.settings.group.bright_blocks"));
        featureLiteral(rows, "chestVisibility", FeatureSwitches.BRIGHT_CHEST,
                text("screen.chisetweaks.settings.copy.bright_chest.description"));
        featureLiteral(rows, "whiteConcreteVisibility", FeatureSwitches.BRIGHT_CONCRETE,
                text("screen.chisetweaks.settings.copy.bright_concrete.description"));
    }

    private static void addBuilderHighlightRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "builderHighlights.visible",
                text("screen.chisetweaks.settings.group.visible_highlights"));
        feature(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS);
        feature(rows, "nether", FeatureSwitches.NETHER_PALETTE);
        feature(rows, "glass", FeatureSwitches.GLASS_INSPECTION);
        feature(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT);

        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "screen.chisetweaks.settings.ore_motion.name",
                "screen.chisetweaks.settings.ore_motion.description");
        action(rows, "moddedOreTargets",
                "screen.chisetweaks.settings.modded_ore.name",
                "screen.chisetweaks.settings.modded_ore.description",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);

        headerLiteral(rows, "builderHighlights.visibleBudget",
                text("screen.chisetweaks.settings.group.local_overlay_budget"));
        configInteger(rows, "highlightRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS, 1);
        configInteger(rows, "highlightVerticalRange", LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS, 1);
        configInteger(rows, "highlightInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL, 5);
        configInteger(rows, "highlightMaxOverlays", LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS, 1);
        configBool(rows, "highlightWorldOverlay", LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY);
        boolLiteral(rows, "highlightDimensionPresets",
                LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS,
                text("screen.chisetweaks.settings.copy.dimension_preset.name"),
                text("screen.chisetweaks.settings.copy.dimension_preset.description"));

        header(rows, "builderHighlights.materialTargets",
                "screen.chisetweaks.settings.section.material_targets");
        addTargets(rows, "visualTargetMaterial");

        headerLiteral(rows, "builderHighlights.occluded",
                text("screen.chisetweaks.settings.group.occluded_highlights"));
        featureLiteral(rows, "lava", FeatureSwitches.LAVA_HIGHLIGHT,
                text("config.comment.locallavahighlight"));
        featureLiteral(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                text("config.comment.hiddensurfacetrace"));

        integer(rows, "occludedRange", LocalFeatureSettings.OCCLUDED_HIGHLIGHT_HORIZONTAL_RADIUS,
                "screen.chisetweaks.settings.occluded_range.name",
                "screen.chisetweaks.settings.occluded_range.description", 1);
        integer(rows, "occludedVerticalRange", LocalFeatureSettings.OCCLUDED_HIGHLIGHT_VERTICAL_RADIUS,
                "screen.chisetweaks.settings.occluded_vertical.name",
                "screen.chisetweaks.settings.occluded_vertical.description", 1);
        integer(rows, "occludedInterval", LocalFeatureSettings.OCCLUDED_HIGHLIGHT_INTERVAL,
                "screen.chisetweaks.settings.occluded_interval.name",
                "screen.chisetweaks.settings.occluded_interval.description", 5);
        integer(rows, "occludedMaxOverlays", LocalFeatureSettings.OCCLUDED_HIGHLIGHT_MAX_OVERLAYS,
                "screen.chisetweaks.settings.occluded_max.name",
                "screen.chisetweaks.settings.occluded_max.description", 1);

        integerLiteral(rows, "hiddenSurfaceColor",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET,
                text("screen.chisetweaks.settings.copy.hidden_color.name"),
                text("screen.chisetweaks.settings.copy.hidden_color.description"), 1);
        integerLiteral(rows, "hiddenSurfaceOpacity",
                LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY,
                text("screen.chisetweaks.settings.copy.hidden_opacity.name"),
                text("screen.chisetweaks.settings.copy.opacity_range"), 5);

        header(rows, "builderHighlights.hiddenTargets",
                "screen.chisetweaks.settings.section.hidden_targets");
        addTargets(rows, "visualTargetHidden");
    }

    private static void addTechnicalVisualizationRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "technical.traces",
                text("screen.chisetweaks.settings.group.technical_traces"));
        feature(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE);
        integerLiteral(rows, "fineThreadColor",
                LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET,
                text("screen.chisetweaks.settings.copy.fine_line_color.name"),
                text("screen.chisetweaks.settings.copy.fine_line_color.description"), 1);
        integerLiteral(rows, "fineThreadOpacity",
                LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY,
                text("screen.chisetweaks.settings.copy.fine_line_opacity.name"),
                text("screen.chisetweaks.settings.copy.opacity_range"), 5);
        addTargets(rows, "visualTargetTechnical");

        headerLiteral(rows, "technical.ranges",
                text("screen.chisetweaks.settings.group.ranges"));
        featureLiteral(rows, "beaconRange", FeatureSwitches.BEACON_RANGE,
                text("screen.chisetweaks.settings.copy.beacon_range.description"));
        featureLiteral(rows, "lightningRodRange", FeatureSwitches.LIGHTNING_ROD_RANGE,
                text("screen.chisetweaks.settings.copy.lightning_rod_range.description"));

        headerLiteral(rows, "technical.links",
                text("screen.chisetweaks.settings.group.links"));
        featureLiteral(rows, "villagerLinks", FeatureSwitches.VILLAGER_ANALYZER,
                text("screen.chisetweaks.settings.copy.villager_links.description"));
    }

    private static void addSceneFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "sceneFilter.targets",
                text("screen.chisetweaks.settings.tab.scene_filter"));
        feature(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS);
        feature(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES);

        headerLiteral(rows, "sceneFilter.behavior",
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

    private static void addIntegrationRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        MasaModAvailability.Snapshot installed = MasaModAvailability.snapshot();
        headerLiteral(rows, "integrations.masa",
                text("screen.chisetweaks.settings.tab.integrations"));
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

        headerLiteral(rows, "masa.installed",
                text("screen.chisetweaks.settings.copy.installed_mods"));
        info(rows, "masa.malilib", "MaLiLib", installedLabel(installed.malilib()));
        info(rows, "masa.litematica", "Litematica", installedLabel(installed.litematica()));
        info(rows, "masa.tweakeroo", "Tweakeroo", installedLabel(installed.tweakeroo()));
        info(rows, "masa.tweakermore", "TweakerMore", installedLabel(installed.tweakermore()));
        info(rows, "masa.syncmatica", "Syncmatica", installedLabel(installed.syncmatica()));

        headerLiteral(rows, "masa.litematica.settings", "Litematica");
        boolLiteral(rows, "schematicPlacementInspector",
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR,
                text("screen.chisetweaks.integrations.litematica_placement.name"),
                text("screen.chisetweaks.integrations.litematica_placement.description"));
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

    private static String installedLabel(boolean installed) {
        return text(installed
                ? "screen.chisetweaks.settings.copy.installed"
                : "screen.chisetweaks.settings.copy.not_installed");
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

    static void integerLiteral(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
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
                "compatibility.renderer.header",
                text("screen.chisetweaks.settings.copy.renderer_compatibility")));
        rows.add(ChiseTweaksSettingRowDefinition.info(
                "compatibility.nvidium.status",
                "Nvidium",
                text(nvidiumInstalled
                        ? "screen.chisetweaks.settings.copy.installed"
                        : "screen.chisetweaks.settings.copy.nvidium_not_installed")));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                "worldBorderFixEnabled",
                text("screen.chisetweaks.settings.copy.world_border_fix.name"),
                text("screen.chisetweaks.settings.copy.world_border_fix.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_ENABLED));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                "worldBorderFixXray",
                text("screen.chisetweaks.settings.copy.world_border_xray.name"),
                text("screen.chisetweaks.settings.copy.world_border_xray.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_XRAY));
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                "worldBorderFixDistance",
                text("screen.chisetweaks.settings.copy.border_distance.name"),
                text("screen.chisetweaks.settings.copy.border_distance.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_DISTANCE,
                16));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                "worldBorderFixFarCoords",
                text("screen.chisetweaks.settings.copy.far_coords.name"),
                text("screen.chisetweaks.settings.copy.far_coords.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_FAR_COORDS));
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                "worldBorderFixCoordThreshold",
                text("screen.chisetweaks.settings.copy.coord_threshold.name"),
                text("screen.chisetweaks.settings.copy.coord_threshold.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_COORD_THRESHOLD,
                1000));
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                "worldBorderFixAutoReenable",
                text("screen.chisetweaks.settings.copy.nvidium_reenable.name"),
                text("screen.chisetweaks.settings.copy.nvidium_reenable.description"),
                CompatibilityIntegrationSettings.WORLD_BORDER_FIX_AUTO_REENABLE));
    }
}
