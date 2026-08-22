package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.Objects;

 
public final class PreReleaseFeaturePolicy {
    private PreReleaseFeaturePolicy() {}

    public static boolean isAvailable(FeatureDefinition definition) {
        FeatureDefinition checked = Objects.requireNonNull(definition, "definition");
        return checked == FeatureDefinition.MATERIAL_HIGHLIGHTS
                || checked == FeatureDefinition.KELP_HIGHLIGHT
                || checked == FeatureDefinition.GLASS_INSPECTION
                || checked == FeatureDefinition.LAVA_HIGHLIGHT
                || checked == FeatureDefinition.ANCIENT_DEBRIS_ANALYZER;
    }
}
