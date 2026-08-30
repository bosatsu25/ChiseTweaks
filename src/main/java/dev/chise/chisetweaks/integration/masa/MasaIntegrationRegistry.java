package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.integration.IntegrationDefinition;

import java.util.List;

/** Canonical optional-integration registry, independent from the 12 runtime FeatureDefinition entries. */
public final class MasaIntegrationRegistry {
    private MasaIntegrationRegistry() {}

    public static List<IntegrationDefinition> definitions() {
        return IntegrationDefinition.VALUES;
    }

    public static boolean isAvailable(
            IntegrationDefinition definition,
            MasaModAvailability.Snapshot availability) {
        if (definition == null || availability == null) return false;
        if (definition == IntegrationDefinition.MASA_GUIDE) return true;
        return availability.loaded(definition.modId());
    }
}
