package dev.chise.chisetweaks.core.definition;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Maps the retained runtime features onto the smaller user-facing Tweaks product model. */
public final class TweaksProductGroupPolicy {
    private static final Map<FeatureDefinition, TweaksProductGroup> GROUPS = groups();

    private TweaksProductGroupPolicy() {}

    public static TweaksProductGroup groupOf(FeatureDefinition feature) {
        TweaksProductGroup group = GROUPS.get(Objects.requireNonNull(feature, "feature"));
        if (group == null) throw new IllegalArgumentException("Unmapped runtime feature: " + feature.id());
        return group;
    }

    private static Map<FeatureDefinition, TweaksProductGroup> groups() {
        EnumMap<FeatureDefinition, TweaksProductGroup> result = new EnumMap<>(FeatureDefinition.class);

        map(result, TweaksProductGroup.SCENE_FILTER,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES);

        map(result, TweaksProductGroup.BUILDER_HIGHLIGHTS,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.HIDDEN_SURFACE_TRACE);

        map(result, TweaksProductGroup.TECHNICAL_VISUALIZATION,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.VILLAGER_ANALYZER,
                FeatureDefinition.BEACON_RANGE,
                FeatureDefinition.LIGHTNING_ROD_RANGE);

        map(result, TweaksProductGroup.VISUAL_TWEAKS,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.HANDHELD_SIZE,
                FeatureDefinition.BRIGHT_CHEST,
                FeatureDefinition.BRIGHT_CONCRETE);

        if (result.size() != FeatureDefinition.VALUES.size()) {
            throw new IllegalStateException("Every runtime feature must belong to exactly one Tweaks product group");
        }
        return Map.copyOf(result);
    }

    private static void map(
            EnumMap<FeatureDefinition, TweaksProductGroup> result,
            TweaksProductGroup group,
            FeatureDefinition... features) {
        for (FeatureDefinition feature : features) {
            if (result.put(feature, group) != null) {
                throw new IllegalStateException("Duplicate Tweaks product mapping: " + feature.id());
            }
        }
    }
}
