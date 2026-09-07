package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.integration.IntegrationDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaModAvailabilityTest {
    @Test
    void missingModsNeverBecomeHardDependencies() {
        var none = new MasaModAvailability.Snapshot(false, false, false, false, false);

        assertFalse(none.integrationAvailable(IntegrationDefinition.LITEMATICA_PICK_REDIRECT));
        assertFalse(none.integrationAvailable(IntegrationDefinition.TWEAKEROO_TOOL_SWITCH_GUARD));
        assertFalse(none.integrationAvailable(IntegrationDefinition.TWEAKERMORE_AUTO_PICK_GUARD));
        assertFalse(none.integrationAvailable(IntegrationDefinition.SYNCMATICA_REMOVE_DISABLED));
        assertFalse(none.integrationAvailable(IntegrationDefinition.SYNCMATICA_REMOVE_REQUIRE_SHIFT));
        assertFalse(none.integrationAvailable(IntegrationDefinition.MASA_JAPANESE_UI));
        assertTrue(none.integrationAvailable(IntegrationDefinition.MASA_GUIDE));
    }

    @Test
    void allInstalledMakesAllExternalIntegrationsAvailable() {
        var all = new MasaModAvailability.Snapshot(true, true, true, true, true);
        for (IntegrationDefinition definition : IntegrationDefinition.VALUES) {
            assertTrue(all.integrationAvailable(definition));
        }
    }
}
