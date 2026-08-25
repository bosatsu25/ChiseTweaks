package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

/** 15個のtoggle可能Featureを一つのregistryで管理する。 */
public final class FeatureSwitches {
    public static final FeatureSwitch AIR_PLACEMENT = create(FeatureDefinition.AIR_PLACEMENT);
    public static final FeatureSwitch BUILDER_FOCUS_BLOCKS = create(FeatureDefinition.BUILDER_FOCUS_BLOCKS);
    public static final FeatureSwitch BUILDER_FOCUS_ENTITIES = create(FeatureDefinition.BUILDER_FOCUS_ENTITIES);
    public static final FeatureSwitch FINE_THREAD_TRACE = create(FeatureDefinition.FINE_THREAD_TRACE);
    public static final FeatureSwitch HIDDEN_SURFACE_TRACE = create(FeatureDefinition.HIDDEN_SURFACE_TRACE);
    public static final FeatureSwitch GLASS_INSPECTION = create(FeatureDefinition.GLASS_INSPECTION);
    public static final FeatureSwitch MATERIAL_HIGHLIGHTS = create(FeatureDefinition.MATERIAL_HIGHLIGHTS);
    public static final FeatureSwitch NETHER_PALETTE = create(FeatureDefinition.NETHER_PALETTE);
    public static final FeatureSwitch KELP_HIGHLIGHT = create(FeatureDefinition.KELP_HIGHLIGHT);

    public static final FeatureSwitch FIRE_VISIBILITY = local(
            FeatureDefinition.FIRE_VISIBILITY,
            "localFireVisibility",
            false,
            config -> config.fireVisibilityEnabled,
            (config, value) -> config.fireVisibilityEnabled = value);
    public static final FeatureSwitch LAVA_HIGHLIGHT = local(
            FeatureDefinition.LAVA_HIGHLIGHT,
            "localLavaHighlight",
            false,
            config -> config.lavaHighlightEnabled,
            (config, value) -> config.lavaHighlightEnabled = value);
    public static final FeatureSwitch ANCIENT_DEBRIS_ANALYZER = local(
            FeatureDefinition.ANCIENT_DEBRIS_ANALYZER,
            "localAncientDebrisAnalyzer",
            false,
            config -> config.ancientDebrisAnalyzerEnabled,
            (config, value) -> config.ancientDebrisAnalyzerEnabled = value);
    public static final FeatureSwitch WARDEN_RISK_ANALYZER = local(
            FeatureDefinition.WARDEN_RISK_ANALYZER,
            "localWardenRiskAnalyzer",
            false,
            config -> config.wardenRiskAnalyzerEnabled,
            (config, value) -> config.wardenRiskAnalyzerEnabled = value);
    public static final FeatureSwitch BRIGHT_CHEST = local(
            FeatureDefinition.BRIGHT_CHEST,
            "brightChest",
            true,
            config -> config.brightChestEnabled,
            (config, value) -> config.brightChestEnabled = value);
    public static final FeatureSwitch BRIGHT_CONCRETE = local(
            FeatureDefinition.BRIGHT_CONCRETE,
            "brightConcrete",
            true,
            config -> config.brightConcreteEnabled,
            (config, value) -> config.brightConcreteEnabled = value);

    /** chisetweaks.jsonに保存されるFeature。 */
    public static final List<FeatureSwitch> FEATURE_CONFIG_VALUES = List.of(
            AIR_PLACEMENT,
            BUILDER_FOCUS_BLOCKS,
            BUILDER_FOCUS_ENTITIES,
            FINE_THREAD_TRACE,
            HIDDEN_SURFACE_TRACE,
            GLASS_INSPECTION,
            MATERIAL_HIGHLIGHTS,
            NETHER_PALETTE,
            KELP_HIGHLIGHT);

    /** chisetweaks-visual.jsonに保存される描画Feature。 */
    public static final List<FeatureSwitch> LOCAL_CONFIG_VALUES = List.of(
            FIRE_VISIBILITY,
            LAVA_HIGHLIGHT,
            ANCIENT_DEBRIS_ANALYZER,
            WARDEN_RISK_ANALYZER,
            BRIGHT_CHEST,
            BRIGHT_CONCRETE);

    /** UI・監査・ドキュメントが参照する15機能の正本。 */
    public static final List<FeatureSwitch> VALUES = List.of(
            AIR_PLACEMENT,
            BUILDER_FOCUS_BLOCKS,
            BUILDER_FOCUS_ENTITIES,
            FINE_THREAD_TRACE,
            HIDDEN_SURFACE_TRACE,
            GLASS_INSPECTION,
            MATERIAL_HIGHLIGHTS,
            NETHER_PALETTE,
            KELP_HIGHLIGHT,
            LAVA_HIGHLIGHT,
            ANCIENT_DEBRIS_ANALYZER,
            WARDEN_RISK_ANALYZER,
            FIRE_VISIBILITY,
            BRIGHT_CHEST,
            BRIGHT_CONCRETE);

    private FeatureSwitches() {}

    private static FeatureSwitch create(FeatureDefinition definition) {
        return new FeatureSwitch(definition);
    }

    private static FeatureSwitch local(
            FeatureDefinition definition,
            String configName,
            boolean defaultEnabled,
            java.util.function.Predicate<LocalFeatureConfig> getter,
            java.util.function.BiConsumer<LocalFeatureConfig, Boolean> setter) {
        return new FeatureSwitch(
                definition,
                configName,
                defaultEnabled,
                () -> getter.test(LocalFeatureConfig.getInstance()),
                value -> setter.accept(LocalFeatureConfig.getInstance(), value),
                SettingPersistence.LOCAL_CONFIG);
    }
}
