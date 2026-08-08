package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.util.List;

/** Persistent MaLiLib configuration for ChiseTweaks feature toggles and rule lists. */
public final class FeatureConfig implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = ChiseTweaksMetadata.MOD_ID + ".json";
    private static boolean sanitizingStringLists;

    public static void onConfigLoaded() {
        sanitizeStringLists();
        BuilderFocusVisibility.buildLists();
        BuilderFocusVisibility.buildEntityLists();
        BuilderFocusVisibility.applyConfig();
    }

    public static void sanitizeStringLists() {
        if (sanitizingStringLists) return;
        sanitizingStringLists = true;
        try {
            for (ConfigStringList option : BuilderFocusConfig.STRING_LIST_OPTIONS) {
                List<String> sanitized = ConfigListPolicy.sanitize(option.getStrings());
                if (!sanitized.equals(option.getStrings())) option.setStrings(sanitized);
            }
        } finally {
            sanitizingStringLists = false;
        }
    }

    public static void loadFromFile() {
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggle.resetForConfigLoad();
        try {
            String raw = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME).orElse(null);
            JsonElement element = null;
            if (raw != null) {
                StrictJsonSecurityPolicy.Validation validation =
                        StrictJsonSecurityPolicy.validateObjectDocument(raw);
                if (!validation.valid()) {
                    throw new JsonParseException("unsafe config JSON: " + validation.reason());
                }
                element = JsonParser.parseString(raw);
            }
            if (element != null && element.isJsonObject()) readFromJson(element.getAsJsonObject());
        } catch (IOException | JsonParseException exception) {
            dev.chise.chisetweaks.ChiseTweaksClient.LOGGER.warn(
                    "ChiseTweaks feature config was not loaded because it was invalid or unsafe");
        }
        onConfigLoaded();
    }

    static void readFromJson(JsonObject root) {
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggle.resetForConfigLoad();
        JsonObject safeRoot = FeatureConfigDocumentPolicy.sanitizeForRead(root);
        ConfigUtils.readConfigBase(safeRoot, "Generic", BuilderFocusConfig.GENERAL_OPTIONS);
        ConfigUtils.readConfigBase(safeRoot, "Lists", BuilderFocusConfig.RULE_OPTIONS);
        ConfigUtils.readHotkeyToggleOptions(
                safeRoot, "FeatureHotkeys", "FeatureToggles", FeatureSwitches.VALUES);
    }

    static JsonObject writeToJson() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "Generic", BuilderFocusConfig.GENERAL_OPTIONS);
        ConfigUtils.writeConfigBase(root, "Lists", BuilderFocusConfig.RULE_OPTIONS);
        ConfigUtils.writeHotkeyToggleOptions(
                root, "FeatureHotkeys", "FeatureToggles", FeatureSwitches.VALUES);
        return root;
    }

    public static void saveToFile() {
        JsonObject root = writeToJson();
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME, root.toString());
        } catch (IOException exception) {
            dev.chise.chisetweaks.ChiseTweaksClient.LOGGER.warn(
                    "ChiseTweaks feature config was not saved because the target was invalid or unsafe");
        }
    }

    @Override public void load() { loadFromFile(); }
    @Override public void save() { saveToFile(); }
}
