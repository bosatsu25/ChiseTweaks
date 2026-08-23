package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** 現行リリースで利用可能な機能を一元管理し、UI配置やruntime状態とは独立して判定する。 */
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
