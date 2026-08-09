package dev.chise.chisetweaks.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Chise-owned persistent configuration for feature toggles and Scene Filter rules. */
public final class FeatureConfig {
    private static final String CONFIG_FILE_NAME = ChiseTweaksMetadata.MOD_ID + ".json";
    private static boolean sanitizingStringLists;

    private FeatureConfig() {}

    public static void onConfigLoaded() {
        sanitizeStringLists();
        BuilderFocusVisibility.applyConfig();
    }

    public static void sanitizeStringLists() {
        if (sanitizingStringLists) return;
        sanitizingStringLists = true;
        try {
            for (ChiseStringListSetting option : BuilderFocusConfig.STRING_LIST_OPTIONS) {
                List<String> sanitized = ConfigListPolicy.sanitize(option.getStrings());
                if (!sanitized.equals(option.getStrings())) option.setStringsSilently(sanitized);
            }
        } finally {
            sanitizingStringLists = false;
        }
    }

    public static void loadFromFile() {
        resetForLoad();
        try {
            String raw = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME).orElse(null);
            if (raw != null) {
                StrictJsonSecurityPolicy.Validation validation =
                        StrictJsonSecurityPolicy.validateObjectDocument(raw);
                if (!validation.valid()) {
                    throw new JsonParseException("unsafe config JSON: " + validation.reason());
                }
                JsonElement element = JsonParser.parseString(raw);
                if (element.isJsonObject()) readFromJson(element.getAsJsonObject());
            }
        } catch (IOException | JsonParseException exception) {
            ChiseTweaksClient.LOGGER.warn(
                    "ChiseTweaks feature config was not loaded because it was invalid or unsafe");
        }
        onConfigLoaded();
    }

    static void readFromJson(JsonObject root) {
        resetForLoad();
        JsonObject safeRoot = FeatureConfigDocumentPolicy.sanitizeForRead(root);

        JsonObject generic = object(safeRoot, "Generic");
        readBoolean(generic, BuilderFocusConfig.REFRESH_RENDERER);

        JsonObject lists = object(safeRoot, "Lists");
        readMode(lists, BuilderFocusConfig.BLOCK_RULE_MODE);
        readStringList(lists, BuilderFocusConfig.BLOCK_WHITELIST);
        readStringList(lists, BuilderFocusConfig.BLOCK_BLACKLIST);
        readMode(lists, BuilderFocusConfig.ENTITY_RULE_MODE);
        readStringList(lists, BuilderFocusConfig.ENTITY_WHITELIST);
        readStringList(lists, BuilderFocusConfig.ENTITY_BLACKLIST);

        JsonObject toggles = object(safeRoot, "FeatureToggles");
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) readBoolean(toggles, toggle);
    }

    static JsonObject writeToJson() {
        JsonObject root = new JsonObject();

        JsonObject generic = new JsonObject();
        generic.addProperty(
                BuilderFocusConfig.REFRESH_RENDERER.getName(),
                BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue());
        root.add("Generic", generic);

        JsonObject lists = new JsonObject();
        writeMode(lists, BuilderFocusConfig.BLOCK_RULE_MODE);
        writeStringList(lists, BuilderFocusConfig.BLOCK_WHITELIST);
        writeStringList(lists, BuilderFocusConfig.BLOCK_BLACKLIST);
        writeMode(lists, BuilderFocusConfig.ENTITY_RULE_MODE);
        writeStringList(lists, BuilderFocusConfig.ENTITY_WHITELIST);
        writeStringList(lists, BuilderFocusConfig.ENTITY_BLACKLIST);
        root.add("Lists", lists);

        JsonObject toggles = new JsonObject();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            toggles.addProperty(toggle.getName(), toggle.getBooleanValue());
        }
        root.add("FeatureToggles", toggles);
        return root;
    }

    public static void saveToFile() {
        sanitizeStringLists();
        JsonObject root = writeToJson();
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME, root.toString());
        } catch (IOException exception) {
            ChiseTweaksClient.LOGGER.warn(
                    "ChiseTweaks feature config was not saved because the target was invalid or unsafe");
        }
    }

    private static void resetForLoad() {
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggle.resetForConfigLoad();
        BuilderFocusConfig.REFRESH_RENDERER.resetSilently();
        BuilderFocusConfig.BLOCK_RULE_MODE.setValueSilently(ChiseRuleMode.NONE);
        BuilderFocusConfig.BLOCK_WHITELIST.setStringsSilently(List.of());
        BuilderFocusConfig.BLOCK_BLACKLIST.setStringsSilently(List.of());
        BuilderFocusConfig.ENTITY_RULE_MODE.setValueSilently(ChiseRuleMode.NONE);
        BuilderFocusConfig.ENTITY_WHITELIST.setStringsSilently(List.of());
        BuilderFocusConfig.ENTITY_BLACKLIST.setStringsSilently(List.of());
    }

    private static JsonObject object(JsonObject root, String key) {
        if (root == null) return new JsonObject();
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static void readBoolean(JsonObject section, ChiseBooleanSetting setting) {
        JsonElement value = section.get(setting.getName());
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            setting.setBooleanValueSilently(value.getAsBoolean());
        }
    }

    private static void readMode(JsonObject section, ChiseRuleModeSetting setting) {
        JsonElement value = section.get(setting.getName());
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            setting.setValueSilently(ChiseRuleMode.parse(value.getAsString()));
        }
    }

    private static void readStringList(JsonObject section, ChiseStringListSetting setting) {
        JsonElement value = section.get(setting.getName());
        if (value == null || !value.isJsonArray()) return;
        ArrayList<String> values = new ArrayList<>();
        for (JsonElement item : value.getAsJsonArray()) {
            if (item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                values.add(item.getAsString());
            }
        }
        setting.setStringsSilently(ConfigListPolicy.sanitize(values));
    }

    private static void writeMode(JsonObject section, ChiseRuleModeSetting setting) {
        section.addProperty(setting.getName(), setting.getValue().storageName());
    }

    private static void writeStringList(JsonObject section, ChiseStringListSetting setting) {
        JsonArray array = new JsonArray();
        for (String value : setting.getStrings()) array.add(value);
        section.add(setting.getName(), array);
    }
}
