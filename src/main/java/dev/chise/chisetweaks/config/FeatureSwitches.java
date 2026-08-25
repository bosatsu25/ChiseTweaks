package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

public final class FeatureSwitches {
    public static final FeatureSwitch BUILDER_FOCUS_BLOCKS = create(
            FeatureDefinition.BUILDER_FOCUS_BLOCKS);
    public static final FeatureSwitch BUILDER_FOCUS_ENTITIES = create(
            FeatureDefinition.BUILDER_FOCUS_ENTITIES);
    public static final FeatureSwitch FINE_THREAD_TRACE = create(
            FeatureDefinition.FINE_THREAD_TRACE);
    public static final FeatureSwitch HIDDEN_SURFACE_TRACE = create(
            FeatureDefinition.HIDDEN_SURFACE_TRACE);
    public static final FeatureSwitch GLASS_INSPECTION = create(
            FeatureDefinition.GLASS_INSPECTION);
    public static final FeatureSwitch MATERIAL_HIGHLIGHTS = create(
            FeatureDefinition.MATERIAL_HIGHLIGHTS);
    public static final FeatureSwitch NETHER_PALETTE = create(
            FeatureDefinition.NETHER_PALETTE);
    public static final FeatureSwitch KELP_HIGHLIGHT = create(
            FeatureDefinition.KELP_HIGHLIGHT);

    public static final List<FeatureSwitch> VALUES = List.of(
            BUILDER_FOCUS_BLOCKS,
            BUILDER_FOCUS_ENTITIES,
            FINE_THREAD_TRACE,
            HIDDEN_SURFACE_TRACE,
            GLASS_INSPECTION,
            MATERIAL_HIGHLIGHTS,
            NETHER_PALETTE,
            KELP_HIGHLIGHT);

    private FeatureSwitches() {}

    private static FeatureSwitch create(FeatureDefinition definition) {
        return new FeatureSwitch(definition);
    }
}
