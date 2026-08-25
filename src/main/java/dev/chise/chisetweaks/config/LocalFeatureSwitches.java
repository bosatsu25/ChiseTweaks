package dev.chise.chisetweaks.config;

/** @deprecated FeatureSwitchesへ統合済み。段階的なsource migration用alias。 */
@Deprecated(forRemoval = true)
public final class LocalFeatureSwitches {
    public static final FeatureSwitch FIRE_VISIBILITY = FeatureSwitches.FIRE_VISIBILITY;
    public static final FeatureSwitch LAVA_HIGHLIGHT = FeatureSwitches.LAVA_HIGHLIGHT;
    public static final FeatureSwitch ANCIENT_DEBRIS_ANALYZER = FeatureSwitches.ANCIENT_DEBRIS_ANALYZER;

    private LocalFeatureSwitches() {}
}
