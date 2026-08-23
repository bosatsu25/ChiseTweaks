package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseFeaturePolicyTest {
    @Test
    void allRetainedVisualFeaturesAreAvailableInCurrentPrerelease() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            assertTrue(PreReleaseFeaturePolicy.isAvailable(definition), definition.id());
        }
    }

    @Test
    void nullDefinitionsAreRejected() {
        assertThrows(NullPointerException.class, () -> PreReleaseFeaturePolicy.isAvailable(null));
    }
}
