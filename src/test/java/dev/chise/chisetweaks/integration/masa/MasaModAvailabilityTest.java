package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.integration.IntegrationDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaModAvailabilityTest {
    private static final List<IntegrationDefinition> MASA_INTEGRATIONS = List.of(
            IntegrationDefinition.LITEMATICA_PICK_REDIRECT,
            IntegrationDefinition.TWEAKEROO_TOOL_SWITCH_GUARD,
            IntegrationDefinition.TWEAKEROO_GAMMA_RESTORE,
            IntegrationDefinition.TWEAKERMORE_AUTO_PICK_GUARD,
            IntegrationDefinition.TWEAKERMORE_MATERIAL_REFRESH,
            IntegrationDefinition.SYNCMATICA_REMOVE_DISABLED,
            IntegrationDefinition.SYNCMATICA_REMOVE_REQUIRE_SHIFT,
            IntegrationDefinition.MASA_JAPANESE_UI,
            IntegrationDefinition.MASA_GUIDE);

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
    void allMasaModsInstalledMakesOnlyMasaIntegrationsAvailable() {
        var all = new MasaModAvailability.Snapshot(true, true, true, true, true);
        for (IntegrationDefinition definition : MASA_INTEGRATIONS) {
            assertTrue(all.integrationAvailable(definition));
        }
        assertFalse(all.integrationAvailable(IntegrationDefinition.NVIDIUM_WORLD_BORDER_FIX));
    }
}
