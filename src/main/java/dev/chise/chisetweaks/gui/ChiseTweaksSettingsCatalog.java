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
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;

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
            case FILTER -> addFilterRows(rows);
            case INSPECTOR -> addInspectorRows(
                    rows,
                    CrosshairInspector.Snapshot.noTarget(),
                    false);
            case ANALYZER -> addAnalyzerRows(rows);
            case VISIBILITY -> addVisibilityRows(rows);
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
        return switch (surface == null ? ChiseTweaksSettingsController.Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> "Highlight";
            case FILTER -> "Filter";
            case INSPECTOR -> "Inspector";
            case ANALYZER -> "Analyzer";
            case VISIBILITY -> "Visibility";
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

    private static void addFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        headerLiteral(rows, "header.filter", "Filter");
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

    private static void addInspectorRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        CrosshairInspector.Snapshot resolved = snapshot == null
                ? CrosshairInspector.Snapshot.noTarget()
                : snapshot;
        header(rows, "inspector.title", "screen.chisetweaks.inspector.title");
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
                info(rows, "inspector.blockState",
                        text("screen.chisetweaks.inspector.block_state"),
                        resolved.stateProperties().isEmpty()
                                ? text("screen.chisetweaks.inspector.none")
                                : String.join("\n", resolved.stateProperties()));
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
        if (includeHelp) addCommonHelpRows(rows);
    }

    private static void addPlacementRows(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            CrosshairInspector.Snapshot placement) {
        header(rows, "placement.title", "screen.chisetweaks.placement.title");
        if (placement.clickedFace() == null) {
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
        var state = placement.predictedPlacement();
        info(rows, "placement.predicted",
                text("screen.chisetweaks.placement.predicted"),
                String.join("\n", CrosshairInspector.placementStateProperties(state)));
        info(rows, "placement.reason",
                text("screen.chisetweaks.inspector.matched_rule"),
                placementReason(placement));
    }

    private static String placementReason(CrosshairInspector.Snapshot placement) {
        return Component.translatable(placement.upperClick()
                        ? "screen.chisetweaks.placement.reason.upper"
                        : "screen.chisetweaks.placement.reason.lower",
                placement.clickedFace().getName()).getString();
    }

    private static String filterReason(BuilderFocusVisibility.FilterDecision decision) {
        String reason = text("screen.chisetweaks.inspector.reason."
                + decision.reason());
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
        boolean throughWall = feature == FeatureDefinition.LAVA_HIGHLIGHT
                || feature == FeatureDefinition.ANCIENT_DEBRIS_ANALYZER;
        return throughWall
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
