package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureAvailabilityPolicyTest {
    @Test
    void everyRetainedFeatureIsAvailableInCurrentRelease() {
        assertEquals(EnumSet.allOf(FeatureDefinition.class), FeatureAvailabilityPolicy.availableFeatures());
        for (FeatureDefinition definition : FeatureDefinition.values()) {
            assertTrue(FeatureAvailabilityPolicy.isAvailable(definition), definition.id());
        }
    }

    @Test
    void nullDefinitionsAreRejected() {
        assertThrows(NullPointerException.class, () -> FeatureAvailabilityPolicy.isAvailable(null));
    }
}
