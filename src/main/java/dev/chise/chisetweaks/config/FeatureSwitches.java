package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

/** Stable registry of Chise-owned user-facing feature switches. */
public final class FeatureSwitches {
    public static final FeatureSwitch PUMPKIN_SCAFFOLD = create(
            FeatureDefinition.PUMPKIN_SCAFFOLD,
            "Temporarily select a hotbar pumpkin for one bounded placement attempt, then restore the original slot.");
    public static final FeatureSwitch BUILDER_FOCUS_BLOCKS = create(
            FeatureDefinition.BUILDER_FOCUS_BLOCKS,
            "Apply explicit block visibility rules for inspection.");
    public static final FeatureSwitch BUILDER_FOCUS_ENTITIES = create(
            FeatureDefinition.BUILDER_FOCUS_ENTITIES,
            "Apply explicit entity visibility rules for inspection.");
    public static final FeatureSwitch FINE_THREAD_TRACE = create(
            FeatureDefinition.FINE_THREAD_TRACE,
            "Trace nearby visible tripwire and hooks within a bounded local radius.");
    public static final FeatureSwitch HIDDEN_SURFACE_TRACE = create(
            FeatureDefinition.HIDDEN_SURFACE_TRACE,
            "Identify nearby visible powder snow, blue ice, dead coral and sculk catalysts.");
    public static final FeatureSwitch GLASS_INSPECTION = create(
            FeatureDefinition.GLASS_INSPECTION,
            "Inspect visible glass boundaries and pane connection state without replacing textures.");
    public static final FeatureSwitch PLACEMENT_GUIDE = create(
            FeatureDefinition.PLACEMENT_GUIDE,
            "Show bounded orientation and placement-state markers for directional construction blocks.");
    public static final FeatureSwitch MATERIAL_HIGHLIGHTS = create(
            FeatureDefinition.MATERIAL_HIGHLIGHTS,
            "Outline visible configured ores, ancient debris and obsidian without wall-through discovery.");
    public static final FeatureSwitch NETHER_PALETTE = create(
            FeatureDefinition.NETHER_PALETTE,
            "Apply bounded color-coded outlines to visible Nether construction materials.");

    public static final List<FeatureSwitch> VALUES = List.of(
            PUMPKIN_SCAFFOLD,
            BUILDER_FOCUS_BLOCKS,
            BUILDER_FOCUS_ENTITIES,
            FINE_THREAD_TRACE,
            HIDDEN_SURFACE_TRACE,
            GLASS_INSPECTION,
            PLACEMENT_GUIDE,
            MATERIAL_HIGHLIGHTS,
            NETHER_PALETTE);

    private FeatureSwitches() {}

    private static FeatureSwitch create(FeatureDefinition definition, String fallbackComment) {
        return new FeatureSwitch(definition, fallbackComment);
    }
}
