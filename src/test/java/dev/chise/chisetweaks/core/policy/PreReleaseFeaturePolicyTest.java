package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreReleaseFeaturePolicyTest {
    @Test
    void releasedVisualFeaturesAreAvailableInCurrentPrerelease() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            if (definition == FeatureDefinition.MATERIAL_HIGHLIGHTS
                    || definition == FeatureDefinition.KELP_HIGHLIGHT
                    || definition == FeatureDefinition.GLASS_INSPECTION
                    || definition == FeatureDefinition.LAVA_HIGHLIGHT
                    || definition == FeatureDefinition.ANCIENT_DEBRIS_ANALYZER) {
                assertTrue(PreReleaseFeaturePolicy.isAvailable(definition), definition.id());
            } else {
                assertFalse(PreReleaseFeaturePolicy.isAvailable(definition), definition.id());
            }
        }
    }

    @Test
    void nullDefinitionsAreRejected() {
        assertThrows(NullPointerException.class, () -> PreReleaseFeaturePolicy.isAvailable(null));
    }
}
