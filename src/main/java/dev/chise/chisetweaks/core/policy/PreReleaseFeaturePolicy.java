package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.Objects;

/**
 * Release gate for the current public pre-release.
 *
 * <p>Ore Highlight, Kelp Highlight, Glass Highlight and Lava Source Highlight are user-operable in
 * this build. Other retained features stay in the source tree for continued development and QA,
 * but must not become effective from UI actions or previously persisted configuration.</p>
 */
public final class PreReleaseFeaturePolicy {
    private PreReleaseFeaturePolicy() {}

    public static boolean isAvailable(FeatureDefinition definition) {
        FeatureDefinition checked = Objects.requireNonNull(definition, "definition");
        return checked == FeatureDefinition.MATERIAL_HIGHLIGHTS
                || checked == FeatureDefinition.KELP_HIGHLIGHT
                || checked == FeatureDefinition.GLASS_INSPECTION
                || checked == FeatureDefinition.LAVA_HIGHLIGHT;
    }
}
