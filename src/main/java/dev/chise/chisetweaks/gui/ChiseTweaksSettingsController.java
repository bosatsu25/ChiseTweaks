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

/**
 * Settings lifecycle, reset and persistence coordinator for the seven Tweaks product groups.
 * Presentation remains in dedicated row models.
 */
final class ChiseTweaksSettingsController {
    enum Surface {
        VISUAL,
        BUILDER_HIGHLIGHTS,
        TECHNICAL_VISUALIZATION,
        SCENE_FILTER,
        BUILDER_ASSIST,
        WORKFLOW,
        INTEGRATIONS
    }

    private final SettingPersistenceCoordinator persistence =
            SettingPersistenceCoordinator.production();
    private final EnumSet<SettingPersistence> dirtyDomains =
            EnumSet.noneOf(SettingPersistence.class);

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

    List<ChiseTweaksSettingRowDefinition> builderAssistRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        return BuilderAssistRows.rows(snapshot, includeHelp);
    }

    List<ChiseTweaksSettingRowDefinition> workflowRows() {
        return WorkflowRows.rows();
    }

    String surfaceTitle(Surface surface) {
        return ChiseTweaksSettingsRows.surfaceTitle(surface);
    }

    EnumSet<SettingPersistence> reset(Surface surface) {
        return switch (surface == null ? Surface.VISUAL : surface) {
            case VISUAL -> {
                resetVisualTweaks();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case BUILDER_HIGHLIGHTS -> {
                resetBuilderHighlights();
                yield EnumSet.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
            case TECHNICAL_VISUALIZATION -> {
                resetTechnicalVisualization();
                yield EnumSet.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
            case SCENE_FILTER -> {
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
                resetBuilderFocusDetails();
                yield EnumSet.of(SettingPersistence.FEATURE_CONFIG);
            }
            case BUILDER_ASSIST -> {
                PatternConsistencyInspector.clearReference();
                yield EnumSet.noneOf(SettingPersistence.class);
            }
            case WORKFLOW -> {
                LocalFeatureSettings.INTERACTION_HISTORY.resetToDefault();
                InteractionHistory.clearHistory();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case INTEGRATIONS -> {
                MasaIntegrationConfig.getInstance().resetToDefaults();
                CompatibilityIntegrationConfig.getInstance().resetToDefaults();
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR.resetToDefault();
                yield EnumSet.of(
                        SettingPersistence.INTEGRATION_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
        };
    }

    void markDirty(SettingPersistence persistenceDomain) {
        if (persistenceDomain != null && persistenceDomain.isApplyManaged()) {
            dirtyDomains.add(persistenceDomain);
        }
    }

    void markDirty(Set<SettingPersistence> persistenceDomains) {
        if (persistenceDomains == null) return;
        for (SettingPersistence persistenceDomain : persistenceDomains) {
            markDirty(persistenceDomain);
        }
    }

    boolean hasPendingChanges() {
        return !dirtyDomains.isEmpty();
    }

    SettingPersistenceCoordinator.SaveResult savePendingConfig() {
        SettingPersistenceCoordinator.SaveResult result = persistence.save(dirtyDomains);
        dirtyDomains.retainAll(result.failedDomains());
        return result;
    }

    Set<SettingPersistence> pendingDomainsForDiagnostics() {
        return Set.copyOf(dirtyDomains);
    }

    private static void resetVisualTweaks() {
        FeatureSwitches.FIRE_VISIBILITY.resetToDefault();
        LocalFeatureSettings.FIRE_VISIBILITY_SIZE.resetToDefault();
        FeatureSwitches.HANDHELD_SIZE.resetToDefault();
        LocalFeatureSettings.HANDHELD_BLOCK_SCALE.resetToDefault();
        LocalFeatureSettings.HANDHELD_ITEM_SCALE.resetToDefault();
        LocalFeatureSettings.HANDHELD_TOOL_SCALE.resetToDefault();
        FeatureSwitches.BRIGHT_CHEST.resetToDefault();
        FeatureSwitches.BRIGHT_CONCRETE.resetToDefault();
    }

    private static void resetBuilderHighlights() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
        FeatureSwitches.NETHER_PALETTE.resetToDefault();
        FeatureSwitches.GLASS_INSPECTION.resetToDefault();
        FeatureSwitches.KELP_HIGHLIGHT.resetToDefault();
        FeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
        FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();

        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY.resetToDefault();
        LocalFeatureSettings.OCCLUDED_HIGHLIGHT_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.OCCLUDED_HIGHLIGHT_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.OCCLUDED_HIGHLIGHT_INTERVAL.resetToDefault();
        LocalFeatureSettings.OCCLUDED_HIGHLIGHT_MAX_OVERLAYS.resetToDefault();

        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
    }

    private static void resetTechnicalVisualization() {
        FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
        FeatureSwitches.VILLAGER_ANALYZER.resetToDefault();
        FeatureSwitches.BEACON_RANGE.resetToDefault();
        FeatureSwitches.LIGHTNING_ROD_RANGE.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.TECHNICAL);
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
