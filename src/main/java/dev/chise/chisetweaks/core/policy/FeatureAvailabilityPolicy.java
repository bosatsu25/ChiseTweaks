package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Current release feature availability. Release status is independent from UI placement and runtime state. */
public final class FeatureAvailabilityPolicy {
    private static final Set<FeatureDefinition> AVAILABLE_FEATURES =
            Collections.unmodifiableSet(EnumSet.allOf(FeatureDefinition.class));

    private FeatureAvailabilityPolicy() {}

    public static boolean isAvailable(FeatureDefinition definition) {
        return AVAILABLE_FEATURES.contains(Objects.requireNonNull(definition, "definition"));
    }

    public static Set<FeatureDefinition> availableFeatures() {
        return AVAILABLE_FEATURES;
    }
}
