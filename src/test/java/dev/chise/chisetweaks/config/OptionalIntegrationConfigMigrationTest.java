package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.policy.MasaJapaneseUiMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OptionalIntegrationConfigMigrationTest {
    @Test
    void masaConfigIgnoresFutureFieldsAndKeepsMissingDefaults() {
        MasaIntegrationConfig config = new MasaIntegrationConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "japaneseUiMode": 1,
                  "litematicaPickRedirect": true,
                  "futureIntegrationFlag": true,
                  "tweakermoreAutoPickBlacklist": [" minecraft:stone ", "minecraft:stone"]
                }
                """));

        assertEquals(1, config.japaneseUiMode);
        assertTrue(config.litematicaPickRedirect);
        assertFalse(config.syncmaticaRemoveRequireShift);
        assertEquals(List.of("minecraft:stone"), config.tweakermoreAutoPickBlacklist);
    }

    @Test
    void masaConfigRejectsIncompatibleTypesAndReturnsToSafeDefaults() {
        MasaIntegrationConfig config = new MasaIntegrationConfig();
        config.litematicaPickRedirect = true;

        assertFalse(config.replaceFromJsonDocument("""
                {"japaneseUiMode": {"unexpected": true}}
                """));

        assertEquals(MasaJapaneseUiMode.AUTO.id(), config.japaneseUiMode);
        assertFalse(config.litematicaPickRedirect);
        assertEquals(3, config.pickRedirectMap.size());
    }

    @Test
    void compatibilityConfigIgnoresFutureFieldsAndSanitizesKnownValues() {
        CompatibilityIntegrationConfig config = new CompatibilityIntegrationConfig();

        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worldBorderFixEnabled": true,
                  "worldBorderFixDistance": 99999,
                  "futureRendererMode": "new"
                }
                """));

        assertTrue(config.worldBorderFixEnabled);
        assertEquals(8192, config.worldBorderFixDistance);
        assertTrue(config.worldBorderFixXray);
        assertTrue(config.worldBorderFixFarCoords);
    }

    @Test
    void compatibilityConfigRejectsIncompatibleTypesAndReturnsToSafeDefaults() {
        CompatibilityIntegrationConfig config = new CompatibilityIntegrationConfig();
        config.worldBorderFixEnabled = true;
        config.worldBorderFixDistance = 512;

        assertFalse(config.replaceFromJsonDocument("""
                {"worldBorderFixDistance": {"unexpected": 512}}
                """));

        assertFalse(config.worldBorderFixEnabled);
        assertEquals(128, config.worldBorderFixDistance);
        assertEquals(100000, config.worldBorderFixCoordThreshold);
    }
}
