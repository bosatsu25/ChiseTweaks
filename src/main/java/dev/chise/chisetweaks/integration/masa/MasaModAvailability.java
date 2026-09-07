package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.integration.IntegrationDefinition;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;

/** Local installed-mod detection for optional Masa ecosystem integrations. */
public final class MasaModAvailability {
    public static final String MALILIB = "malilib";
    public static final String LITEMATICA = "litematica";
    public static final String TWEAKEROO = "tweakeroo";
    public static final String TWEAKERMORE = "tweakermore";
    public static final String SYNCMATICA = "syncmatica";

    private MasaModAvailability() {}

    public static boolean isLoaded(String modId) {
        return modId != null && !modId.isBlank() && FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                isLoaded(MALILIB),
                isLoaded(LITEMATICA),
                isLoaded(TWEAKEROO),
                isLoaded(TWEAKERMORE),
                isLoaded(SYNCMATICA));
    }

    public record Snapshot(
            boolean malilib,
            boolean litematica,
            boolean tweakeroo,
            boolean tweakermore,
            boolean syncmatica) {

        public boolean loaded(String modId) {
            return switch (modId == null ? "" : modId) {
                case MALILIB -> malilib;
                case LITEMATICA -> litematica;
                case TWEAKEROO -> tweakeroo;
                case TWEAKERMORE -> tweakermore;
                case SYNCMATICA -> syncmatica;
                default -> false;
            };
        }

        public boolean integrationAvailable(IntegrationDefinition definition) {
            if (definition == null) return false;
            if (definition == IntegrationDefinition.MASA_GUIDE) return true;
            if (definition == IntegrationDefinition.MASA_JAPANESE_UI) {
                return malilib || litematica || tweakeroo || tweakermore || syncmatica;
            }
            return loaded(definition.modId());
        }

        public Map<String, Boolean> asMap() {
            return Map.of(
                    MALILIB, malilib,
                    LITEMATICA, litematica,
                    TWEAKEROO, tweakeroo,
                    TWEAKERMORE, tweakermore,
                    SYNCMATICA, syncmatica);
        }
    }
}
