package dev.chise.chisetweaks.config;

/** @deprecated Bright Concreteは通常Featureへ統合済み。段階的なsource migration用alias。 */
@Deprecated(forRemoval = true)
public final class WhiteConcreteVisibilitySetting {
    public static final FeatureSwitch INSTANCE = FeatureSwitches.BRIGHT_CONCRETE;

    private WhiteConcreteVisibilitySetting() {}
}
