package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.integration.IntegrationDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaIntegrationRegistryTest {
    @Test
    void missingModsNeverBecomeHardDependencies() {
        var none = new MasaModAvailability.Snapshot(false, false, false, false, false);

        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.LITEMATICA_PICK_REDIRECT, none));
        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.TWEAKEROO_TOOL_SWITCH_GUARD, none));
        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.TWEAKERMORE_AUTO_PICK_GUARD, none));
        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.SYNCMATICA_REMOVE_DISABLED, none));
        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.SYNCMATICA_REMOVE_REQUIRE_SHIFT, none));
        assertFalse(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.MASA_JAPANESE_UI, none));
        assertTrue(MasaIntegrationRegistry.isAvailable(
                IntegrationDefinition.MASA_GUIDE, none));
    }

    @Test
    void allInstalledMakesAllExternalIntegrationsAvailable() {
        var all = new MasaModAvailability.Snapshot(true, true, true, true, true);
        for (IntegrationDefinition definition : MasaIntegrationRegistry.definitions()) {
            assertTrue(MasaIntegrationRegistry.isAvailable(definition, all));
        }
    }
}
