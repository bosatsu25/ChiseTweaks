package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

/** toggle可能Featureを一つのregistryで管理する。 */
public final class FeatureSwitches {
    public static final FeatureSwitch BUILDER_FOCUS_BLOCKS = create(FeatureDefinition.BUILDER_FOCUS_BLOCKS);
    public static final FeatureSwitch BUILDER_FOCUS_ENTITIES = create(FeatureDefinition.BUILDER_FOCUS_ENTITIES);
    public static final FeatureSwitch FINE_THREAD_TRACE = create(FeatureDefinition.FINE_THREAD_TRACE);
    public static final FeatureSwitch HIDDEN_SURFACE_TRACE = create(FeatureDefinition.HIDDEN_SURFACE_TRACE);
    public static final FeatureSwitch GLASS_INSPECTION = create(FeatureDefinition.GLASS_INSPECTION);
    public static final FeatureSwitch MATERIAL_HIGHLIGHTS = create(FeatureDefinition.MATERIAL_HIGHLIGHTS);
    public static final FeatureSwitch NETHER_PALETTE = create(FeatureDefinition.NETHER_PALETTE);
    public static final FeatureSwitch KELP_HIGHLIGHT = create(FeatureDefinition.KELP_HIGHLIGHT);

    public static final FeatureSwitch FIRE_VISIBILITY =
            local(FeatureDefinition.FIRE_VISIBILITY, "localFireVisibility", false);
    public static final FeatureSwitch HANDHELD_SIZE =
            local(FeatureDefinition.HANDHELD_SIZE, "handheldSize", false);
    public static final FeatureSwitch LAVA_HIGHLIGHT =
            local(FeatureDefinition.LAVA_HIGHLIGHT, "localLavaHighlight", false);
    public static final FeatureSwitch VILLAGER_ANALYZER =
            local(FeatureDefinition.VILLAGER_ANALYZER, "villagerAnalyzer", false);
    public static final FeatureSwitch BEACON_RANGE =
            local(FeatureDefinition.BEACON_RANGE, "beaconRange", false);
    public static final FeatureSwitch LIGHTNING_ROD_RANGE =
            local(FeatureDefinition.LIGHTNING_ROD_RANGE, "lightningRodRange", false);
    public static final FeatureSwitch BRIGHT_CHEST =
            local(FeatureDefinition.BRIGHT_CHEST, "brightChest", true);
    public static final FeatureSwitch BRIGHT_CONCRETE =
            local(FeatureDefinition.BRIGHT_CONCRETE, "brightConcrete", true);

    /** chisetweaks.jsonに保存されるFeature。 */
    public static final List<FeatureSwitch> FEATURE_CONFIG_VALUES = List.of(
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
            HANDHELD_SIZE,
            LAVA_HIGHLIGHT,
            VILLAGER_ANALYZER,
            BEACON_RANGE,
            LIGHTNING_ROD_RANGE,
            BRIGHT_CHEST,
            BRIGHT_CONCRETE);

    /** UI・監査・ドキュメントが参照するruntime機能の正本。 */
    public static final List<FeatureSwitch> VALUES = List.of(
            BUILDER_FOCUS_BLOCKS,
            BUILDER_FOCUS_ENTITIES,
            FINE_THREAD_TRACE,
            HIDDEN_SURFACE_TRACE,
            GLASS_INSPECTION,
            MATERIAL_HIGHLIGHTS,
            NETHER_PALETTE,
            KELP_HIGHLIGHT,
            HANDHELD_SIZE,
            LAVA_HIGHLIGHT,
            VILLAGER_ANALYZER,
            BEACON_RANGE,
            LIGHTNING_ROD_RANGE,
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
            boolean defaultEnabled) {
        return new FeatureSwitch(
                definition,
                configName,
                defaultEnabled,
                () -> LocalFeatureConfig.getInstance().featureEnabled(definition),
                value -> LocalFeatureConfig.getInstance().setFeatureEnabled(definition, value),
                SettingPersistence.LOCAL_CONFIG);
    }
}
