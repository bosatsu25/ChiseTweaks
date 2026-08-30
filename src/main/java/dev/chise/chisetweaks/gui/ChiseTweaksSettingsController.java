package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.CompatibilityIntegrationConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Settings lifecycle, reset and persistence coordinator. Presentation lives in dedicated row models. */
final class ChiseTweaksSettingsController {
    /**
     * Stable internal surface identifiers. User-facing meaning is provided by
     * {@link TweaksProductSettingsRows} so existing layout/navigation contracts stay migration-safe.
     */
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
    private final EnumSet<SettingPersistence> dirtyDomains =
            EnumSet.noneOf(SettingPersistence.class);

    static ChiseTweaksSettingsController forCurrentLanguage() {
        return new ChiseTweaksSettingsController();
    }

    /** Setting bindings initialize on first use; this remains the screen lifecycle hook. */
    void initialize() {}

    List<ChiseTweaksSettingRowDefinition> rows() {
        return TweaksProductSettingsRows.rows(Surface.HIGHLIGHT);
    }

    List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        return TweaksProductSettingsRows.rows(surface);
    }

    List<ChiseTweaksSettingRowDefinition> inspectorRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        return TweaksBuilderAssistRows.rows(snapshot, includeHelp);
    }

    String surfaceTitle(Surface surface) {
        return TweaksProductSettingsRows.title(surface);
    }

    EnumSet<SettingPersistence> reset(Surface surface) {
        return switch (surface == null ? Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> {
                // Builder Highlights: material/shape highlights plus bounded occluded highlights.
                FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
                FeatureSwitches.NETHER_PALETTE.resetToDefault();
                FeatureSwitches.GLASS_INSPECTION.resetToDefault();
                FeatureSwitches.KELP_HIGHLIGHT.resetToDefault();
                FeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
                FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
                LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
                resetOccludedHighlightDetails();
                resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
                resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
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
                // Builder Assist keeps preview/pattern logic stateless; only optional history/schematic flags persist.
                LocalFeatureSettings.INTERACTION_HISTORY.resetToDefault();
                LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR.resetToDefault();
                InteractionHistory.clearHistory();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case ANALYZER -> {
                // Technical Visualization: traces, ranges and known villager job-site links.
                FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
                FeatureSwitches.VILLAGER_ANALYZER.resetToDefault();
                FeatureSwitches.BEACON_RANGE.resetToDefault();
                FeatureSwitches.LIGHTNING_ROD_RANGE.resetToDefault();
                resetTechnicalVisualizationDetails();
                yield EnumSet.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG);
            }
            case VISIBILITY -> {
                // Visual Tweaks: first-person comfort and simple bright rendering only.
                FeatureSwitches.FIRE_VISIBILITY.resetToDefault();
                LocalFeatureSettings.FIRE_VISIBILITY_SIZE.resetToDefault();
                FeatureSwitches.HANDHELD_SIZE.resetToDefault();
                LocalFeatureSettings.HANDHELD_BLOCK_SCALE.resetToDefault();
                LocalFeatureSettings.HANDHELD_ITEM_SCALE.resetToDefault();
                LocalFeatureSettings.HANDHELD_TOOL_SCALE.resetToDefault();
                FeatureSwitches.BRIGHT_CHEST.resetToDefault();
                FeatureSwitches.BRIGHT_CONCRETE.resetToDefault();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case INTEGRATIONS -> {
                MasaIntegrationConfig.getInstance().resetToDefaults();
                CompatibilityIntegrationConfig.getInstance().resetToDefaults();
                yield EnumSet.of(SettingPersistence.INTEGRATION_CONFIG);
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

    private static void resetTechnicalVisualizationDetails() {
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.TECHNICAL);
    }

    private static void resetOccludedHighlightDetails() {
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
            LocalFeatureSettings.setAllOreHighlightTargets(true);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask =
                VisualTargetGroupPolicy.withAll(config.visualTargetMask, group, true);
    }
}
