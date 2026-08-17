package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

/** Stable registry of the retained Chise-owned user-facing feature switches. */
public final class FeatureSwitches {
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
            "Add sparse fullbright shape markers to vanilla glass blocks and panes without replacing their base model or stained color.");
    public static final FeatureSwitch MATERIAL_HIGHLIGHTS = create(
            FeatureDefinition.MATERIAL_HIGHLIGHTS,
            "Outline visible configured ores, ancient debris and obsidian without wall-through discovery.");
    public static final FeatureSwitch NETHER_PALETTE = create(
            FeatureDefinition.NETHER_PALETTE,
            "Apply bounded color-coded outlines to visible Nether construction materials.");
    public static final FeatureSwitch KELP_HIGHLIGHT = create(
            FeatureDefinition.KELP_HIGHLIGHT,
            "Highlight visible kelp and kelp plants with a magenta and orange neon overlay.");

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

    private static FeatureSwitch create(FeatureDefinition definition, String fallbackComment) {
        return new FeatureSwitch(definition, fallbackComment);
    }
}
