package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.CompatibilityIntegrationConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Settings lifecycle, reset and persistence coordinator. Presentation lives in dedicated row models. */
final class ChiseTweaksSettingsController {
    enum Surface {
        HIGHLIGHT,
        FILTER,
        INSPECTOR,
        ANALYZER,
        VISIBILITY,
        INTEGRATIONS
    }

    private final SettingPersistenceCoordinator persistence =
            SettingPersistenceCoordinator.production();

    static ChiseTweaksSettingsController forCurrentLanguage() {
        return new ChiseTweaksSettingsController();
    }

    /** Setting bindings initialize on first use; this remains the screen lifecycle hook. */
    void initialize() {}

    List<ChiseTweaksSettingRowDefinition> rows() {
        return ChiseTweaksSettingsRows.rows();
    }

    List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        return ChiseTweaksSettingsRows.rows(surface);
    }

    List<ChiseTweaksSettingRowDefinition> inspectorRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        return InspectorSettingsRows.rows(snapshot, includeHelp);
    }

    String surfaceTitle(Surface surface) {
        return ChiseTweaksSettingsRows.surfaceTitle(surface);
    }

    EnumSet<SettingPersistence> reset(Surface surface) {
        return switch (surface == null ? Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> {
                resetHighlightFeatures();
                resetHighlightDetails();
                yield EnumSet.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
            case FILTER -> {
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
                resetBuilderFocusDetails();
                yield EnumSet.of(SettingPersistence.FEATURE_CONFIG);
            }
            case INSPECTOR -> {
                LocalFeatureSettings.INTERACTION_HISTORY.resetToDefault();
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR.resetToDefault();
                InteractionHistory.clearHistory();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case ANALYZER -> {
                FeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
                FeatureSwitches.VILLAGER_ANALYZER.resetToDefault();
                FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
                resetAnalyzerDetails();
                yield EnumSet.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
            case VISIBILITY -> {
                FeatureSwitches.FIRE_VISIBILITY.resetToDefault();
                LocalFeatureSettings.FIRE_VISIBILITY_SIZE.resetToDefault();
                FeatureSwitches.BRIGHT_CHEST.resetToDefault();
                FeatureSwitches.BRIGHT_CONCRETE.resetToDefault();
                FeatureSwitches.BEACON_RANGE.resetToDefault();
                FeatureSwitches.LIGHTNING_ROD_RANGE.resetToDefault();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case INTEGRATIONS -> {
                MasaIntegrationConfig.getInstance().resetToDefaults();
                CompatibilityIntegrationConfig.getInstance().resetToDefaults();
                yield EnumSet.of(SettingPersistence.INTEGRATION_CONFIG);
            }
        };
    }

    SettingPersistenceCoordinator.SaveResult saveConfig(
            Set<SettingPersistence> dirtyDomains) {
        return persistence.save(dirtyDomains);
    }

    private static void resetHighlightFeatures() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
        FeatureSwitches.NETHER_PALETTE.resetToDefault();
        FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
        FeatureSwitches.GLASS_INSPECTION.resetToDefault();
        FeatureSwitches.KELP_HIGHLIGHT.resetToDefault();
    }

    private static void resetHighlightDetails() {
        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.TECHNICAL);
    }

    private static void resetAnalyzerDetails() {
        LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_INTERVAL.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.HIDDEN_ANALYZER_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.HIDDEN_ANALYZER_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.HIDDEN_ANALYZER_INTERVAL.resetToDefault();
        LocalFeatureSettings.HIDDEN_ANALYZER_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
    }

    private static void resetBuilderFocusDetails() {
        BuilderFocusConfig.REFRESH_RENDERER.resetToDefault();
        BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault();
        BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault();
        BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault();
        BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault();
        BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault();
        BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault();
    }

    private static void resetTargetGroup(VisualTargetGroupPolicy.Group group) {
        if (group == VisualTargetGroupPolicy.Group.MATERIAL) {
            VisualTargetSettings.setAllOreHighlightTargets(true);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask =
                VisualTargetGroupPolicy.withAll(config.visualTargetMask, group, true);
    }
}
