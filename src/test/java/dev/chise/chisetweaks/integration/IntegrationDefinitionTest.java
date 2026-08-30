package dev.chise.chisetweaks.integration;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntegrationDefinitionTest {
    @Test
    void integrationIdsRemainUniqueAndSeparateFromRuntimeFeatures() {
        var ids = new HashSet<String>();
        for (IntegrationDefinition definition : IntegrationDefinition.VALUES) {
            assertTrue(ids.add(definition.id()));
            assertTrue(!definition.modId().isBlank());
            assertTrue(!definition.englishName().isBlank());
        }
        assertEquals(9, ids.size());
    }
}
