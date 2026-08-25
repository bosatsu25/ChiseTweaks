package dev.chise.chisetweaks.config;

/** @deprecated Bright Chestは通常Featureへ統合済み。段階的なsource migration用alias。 */
@Deprecated(forRemoval = true)
public final class ChestVisibilitySetting {
    public static final FeatureSwitch INSTANCE = FeatureSwitches.BRIGHT_CHEST;

    private ChestVisibilitySetting() {}
}
