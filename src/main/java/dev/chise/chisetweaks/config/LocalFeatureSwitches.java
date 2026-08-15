package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

/** Stable registry for local-config-backed user-facing switches. */
public final class LocalFeatureSwitches {
    public static final LocalFeatureSwitch FIRE_VISIBILITY = new LocalFeatureSwitch(
            FeatureDefinition.FIRE_VISIBILITY,
            "localFireVisibility",
            config -> config.fireVisibilityEnabled,
            (config, value) -> config.fireVisibilityEnabled = value);

    public static final LocalFeatureSwitch LAVA_HIGHLIGHT = new LocalFeatureSwitch(
            FeatureDefinition.LAVA_HIGHLIGHT,
            "localLavaHighlight",
            config -> config.lavaHighlightEnabled,
            (config, value) -> config.lavaHighlightEnabled = value);

    public static final List<LocalFeatureSwitch> VALUES = List.of(FIRE_VISIBILITY, LAVA_HIGHLIGHT);

    private LocalFeatureSwitches() {}

}
