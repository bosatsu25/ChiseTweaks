package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

/** @deprecated Use {@link FeatureAvailabilityPolicy}. */
@Deprecated(forRemoval = true)
public final class PreReleaseFeaturePolicy {
    private PreReleaseFeaturePolicy() {}

    public static boolean isAvailable(FeatureDefinition definition) {
        return FeatureAvailabilityPolicy.isAvailable(definition);
    }
}
