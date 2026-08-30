package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.config.MasaIntegrationSettings;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 設定操作だけを調整し、表示用メタデータは{@link ChiseTweaksSettingsCatalog}へ分離するcontroller。 */
final class ChiseTweaksSettingsController {
    enum Surface {
        HIGHLIGHT,
        FILTER,
        INSPECTOR,
        ANALYZER,
        VISIBILITY,
        INTEGRATIONS
    }

    private final ChiseTweaksSettingsCatalog catalog = new ChiseTweaksSettingsCatalog();
    private final SettingPersistenceCoordinator persistence = SettingPersistenceCoordinator.production();

    static ChiseTweaksSettingsController forCurrentLanguage() {
        return new ChiseTweaksSettingsController();
    }

    /** 設定bindingは初回参照時に初期化されるため、画面ライフサイクル用hookだけを保持する。 */
    void initialize() {}

    List<ChiseTweaksSettingRowDefinition> rows() {
        return rows(Surface.HIGHLIGHT);
    }

    List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        return catalog.rows(surface == null ? Surface.HIGHLIGHT : surface);
    }

    List<ChiseTweaksSettingRowDefinition> inspectorRows(
            CrosshairInspector.Snapshot snapshot,
            boolean includeHelp) {
        return catalog.inspectorRows(snapshot, includeHelp);
    }

    String surfaceTitle(Surface surface) {
        return catalog.surfaceTitle(surface);
    }

    EnumSet<SettingPersistence> reset(Surface surface) {
        return switch (surface == null ? Surface.HIGHLIGHT : surface) {
            case HIGHLIGHT -> {
                resetHighlightFeatures();
                resetHighlightDetails();
                yield EnumSet.of(SettingPersistence.FEATURE_CONFIG, SettingPersistence.LOCAL_CONFIG);
            }
            case FILTER -> {
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
                resetBuilderFocusDetails();
                yield EnumSet.of(SettingPersistence.FEATURE_CONFIG);
            }
            case INSPECTOR -> EnumSet.noneOf(SettingPersistence.class);
            case ANALYZER -> {
                FeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
                resetAnalyzerDetails();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case VISIBILITY -> {
                FeatureSwitches.FIRE_VISIBILITY.resetToDefault();
                LocalFeatureSettings.FIRE_VISIBILITY_SIZE.resetToDefault();
                FeatureSwitches.BRIGHT_CHEST.resetToDefault();
                FeatureSwitches.BRIGHT_CONCRETE.resetToDefault();
                yield EnumSet.of(SettingPersistence.LOCAL_CONFIG);
            }
            case INTEGRATIONS -> {
                MasaIntegrationConfig.getInstance().resetToDefaults();
                yield EnumSet.of(SettingPersistence.INTEGRATION_CONFIG);
            }
        };
    }

    SettingPersistenceCoordinator.SaveResult saveConfig(Set<SettingPersistence> dirtyDomains) {
        return persistence.save(dirtyDomains);
    }

    private static void resetHighlightFeatures() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
        FeatureSwitches.NETHER_PALETTE.resetToDefault();
        FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
        FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
        FeatureSwitches.GLASS_INSPECTION.resetToDefault();
        FeatureSwitches.KELP_HIGHLIGHT.resetToDefault();
    }

    private void resetHighlightDetails() {
        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.TECHNICAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
    }

    private void resetAnalyzerDetails() {
        LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_INTERVAL.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS.resetToDefault();
    }

    private void resetBuilderFocusDetails() {
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
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(config.visualTargetMask, group, true);
    }
}
