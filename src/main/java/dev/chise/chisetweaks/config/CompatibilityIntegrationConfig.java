package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/** Persistence domain for optional renderer/runtime compatibility integrations. */
public final class CompatibilityIntegrationConfig {
    static final String CONFIG_FILE_NAME = "chisetweaks-compatibility.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final CompatibilityIntegrationConfig INSTANCE = new CompatibilityIntegrationConfig();

    public boolean worldBorderFixEnabled;
    public boolean worldBorderFixXray = true;
    public int worldBorderFixDistance = 128;
    public boolean worldBorderFixFarCoords = true;
    public int worldBorderFixCoordThreshold = 100000;
    public boolean worldBorderFixAutoReenable;

    CompatibilityIntegrationConfig() {}

    public static CompatibilityIntegrationConfig getInstance() {
        return INSTANCE;
    }

    public synchronized boolean load() {
        try {
            Optional<String> document = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME);
            if (document.isEmpty()) {
                resetToDefaults();
                return true;
            }
            if (replaceFromJsonDocument(document.get())) return true;
            ChiseTweaksClient.LOGGER.warn("Rejected compatibility integration config; using safe defaults");
            return false;
        } catch (java.io.IOException | RuntimeException failure) {
            resetToDefaults();
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to load compatibility integration config after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    boolean replaceFromJsonDocument(String json) {
        resetToDefaults();
        if (json == null || json.isBlank()) return false;
        StrictJsonSecurityPolicy.Validation validation =
                StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) return false;
        try {
            JsonObject source = JsonParser.parseString(json).getAsJsonObject();
            CompatibilityIntegrationConfig loaded =
                    GSON.fromJson(source, CompatibilityIntegrationConfig.class);
            if (loaded == null) return false;
            copyFrom(loaded);
            sanitize();
            return true;
        } catch (RuntimeException failure) {
            resetToDefaults();
            return false;
        }
    }

    public synchronized boolean save() {
        sanitize();
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME, GSON.toJson(this));
            return true;
        } catch (java.io.IOException | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to save compatibility integration config after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    public synchronized void resetToDefaults() {
        copyFrom(new CompatibilityIntegrationConfig());
    }

    public synchronized void sanitize() {
        worldBorderFixDistance = Math.max(1, Math.min(8192, worldBorderFixDistance));
        worldBorderFixCoordThreshold = Math.max(1000, Math.min(29999999, worldBorderFixCoordThreshold));
    }

    private void copyFrom(CompatibilityIntegrationConfig source) {
        worldBorderFixEnabled = source.worldBorderFixEnabled;
        worldBorderFixXray = source.worldBorderFixXray;
        worldBorderFixDistance = source.worldBorderFixDistance;
        worldBorderFixFarCoords = source.worldBorderFixFarCoords;
        worldBorderFixCoordThreshold = source.worldBorderFixCoordThreshold;
        worldBorderFixAutoReenable = source.worldBorderFixAutoReenable;
    }
}
