package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.policy.MasaJapaneseUiMode;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Standalone persistence domain for optional Masa ecosystem integrations. */
public final class MasaIntegrationConfig {
    static final String CONFIG_FILE_NAME = "chisetweaks-integrations.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_LIST_ENTRIES = 128;
    private static final int MAX_ENTRY_CHARS = 160;

    private static final MasaIntegrationConfig INSTANCE = new MasaIntegrationConfig();

    public int japaneseUiMode = MasaJapaneseUiMode.AUTO.id();

    public boolean litematicaPickRedirect;
    public boolean tweakerooToolSwitchGuard;
    public boolean tweakerooPersistentGammaOverride;
    public boolean tweakermoreAutoPickGuard;
    public boolean tweakermoreMaterialListRefresh;
    public boolean syncmaticaRemoveDisabled;
    public boolean syncmaticaRemoveRequireShift;

    public int tweakermoreAutoPickListMode;
    public int tweakerooToolSwitchListMode;

    public List<String> pickRedirectMap = new ArrayList<>(List.of(
            "minecraft:farmland,minecraft:dirt",
            "minecraft:dirt_path,minecraft:dirt",
            "minecraft:water,minecraft:ice"));
    public List<String> tweakermoreAutoPickWhitelist = new ArrayList<>();
    public List<String> tweakermoreAutoPickBlacklist = new ArrayList<>(List.of(
            "minecraft:golden_carrot",
            "minecraft:ender_chest",
            "minecraft:shulker_box",
            "minecraft:totem_of_undying"));
    public List<String> tweakerooToolSwitchWhitelist = new ArrayList<>();
    public List<String> tweakerooToolSwitchBlacklist = new ArrayList<>();

    MasaIntegrationConfig() {}

    public static MasaIntegrationConfig getInstance() {
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
            ChiseTweaksClient.LOGGER.warn("Rejected Masa integration config; using safe defaults");
            return false;
        } catch (java.io.IOException | RuntimeException failure) {
            resetToDefaults();
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to load Masa integration config after {}",
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
            MasaIntegrationConfig loaded = GSON.fromJson(source, MasaIntegrationConfig.class);
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
                    "Unable to save Masa integration config after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    public synchronized void resetToDefaults() {
        copyFrom(new MasaIntegrationConfig());
    }

    public synchronized void sanitize() {
        japaneseUiMode = MasaJapaneseUiMode.fromId(japaneseUiMode).id();
        tweakermoreAutoPickListMode = clampListMode(tweakermoreAutoPickListMode);
        tweakerooToolSwitchListMode = clampListMode(tweakerooToolSwitchListMode);
        pickRedirectMap = sanitizeList(pickRedirectMap);
        tweakermoreAutoPickWhitelist = sanitizeList(tweakermoreAutoPickWhitelist);
        tweakermoreAutoPickBlacklist = sanitizeList(tweakermoreAutoPickBlacklist);
        tweakerooToolSwitchWhitelist = sanitizeList(tweakerooToolSwitchWhitelist);
        tweakerooToolSwitchBlacklist = sanitizeList(tweakerooToolSwitchBlacklist);
    }

    private void copyFrom(MasaIntegrationConfig source) {
        japaneseUiMode = source.japaneseUiMode;
        litematicaPickRedirect = source.litematicaPickRedirect;
        tweakerooToolSwitchGuard = source.tweakerooToolSwitchGuard;
        tweakerooPersistentGammaOverride = source.tweakerooPersistentGammaOverride;
        tweakermoreAutoPickGuard = source.tweakermoreAutoPickGuard;
        tweakermoreMaterialListRefresh = source.tweakermoreMaterialListRefresh;
        syncmaticaRemoveDisabled = source.syncmaticaRemoveDisabled;
        syncmaticaRemoveRequireShift = source.syncmaticaRemoveRequireShift;
        tweakermoreAutoPickListMode = source.tweakermoreAutoPickListMode;
        tweakerooToolSwitchListMode = source.tweakerooToolSwitchListMode;
        pickRedirectMap = copyList(source.pickRedirectMap);
        tweakermoreAutoPickWhitelist = copyList(source.tweakermoreAutoPickWhitelist);
        tweakermoreAutoPickBlacklist = copyList(source.tweakermoreAutoPickBlacklist);
        tweakerooToolSwitchWhitelist = copyList(source.tweakerooToolSwitchWhitelist);
        tweakerooToolSwitchBlacklist = copyList(source.tweakerooToolSwitchBlacklist);
    }

    private static int clampListMode(int value) {
        return Math.max(0, Math.min(2, value));
    }

    private static List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static List<String> sanitizeList(List<String> source) {
        ArrayList<String> result = new ArrayList<>();
        if (source == null) return result;
        for (String value : source) {
            if (result.size() >= MAX_LIST_ENTRIES) break;
            if (value == null) continue;
            String normalized = value.trim();
            if (normalized.isEmpty()) continue;
            if (normalized.length() > MAX_ENTRY_CHARS) normalized = normalized.substring(0, MAX_ENTRY_CHARS);
            if (!result.contains(normalized)) result.add(normalized);
        }
        return result;
    }
}
