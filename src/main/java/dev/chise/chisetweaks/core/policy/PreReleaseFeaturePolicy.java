package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.Objects;

/**
 * Release gate for the current public pre-release.
 *
 * <p>Only Ore Highlight is user-operable in this build. All other retained features stay in the
 * source tree for continued development and QA, but must not become effective from UI actions or
 * previously persisted configuration.</p>
 */
public final class PreReleaseFeaturePolicy {
    private PreReleaseFeaturePolicy() {}

    public static boolean isAvailable(FeatureDefinition definition) {
        return Objects.requireNonNull(definition, "definition") == FeatureDefinition.MATERIAL_HIGHLIGHTS;
    }
}
