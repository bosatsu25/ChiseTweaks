package dev.chise.chisetweaks.gui.help;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.security.StrictUtf8;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Bounded, read-only Japanese/English text catalog used only by the feature guide. */
public final class FeatureHelpLanguageCatalog {
    private static final int MAX_LANGUAGE_BYTES = 256 * 1024;
    private static final Map<FeatureHelpDisplayLanguage, Map<String, String>> TEXTS = loadAll();

    private FeatureHelpLanguageCatalog() {
    }

    public static String text(FeatureHelpDisplayLanguage language, String key, String fallback) {
        FeatureHelpDisplayLanguage safeLanguage = Objects.requireNonNullElse(
                language, FeatureHelpDisplayLanguage.ENGLISH);
        String safeKey = Objects.requireNonNullElse(key, "");
        String value = TEXTS.getOrDefault(safeLanguage, Map.of()).get(safeKey);
        return value == null || value.isBlank() ? Objects.requireNonNullElse(fallback, "") : value;
    }

    public static boolean contains(FeatureHelpDisplayLanguage language, String key) {
        return TEXTS.getOrDefault(language, Map.of()).containsKey(Objects.requireNonNullElse(key, ""));
    }

    private static Map<FeatureHelpDisplayLanguage, Map<String, String>> loadAll() {
        EnumMap<FeatureHelpDisplayLanguage, Map<String, String>> result =
                new EnumMap<>(FeatureHelpDisplayLanguage.class);
        for (FeatureHelpDisplayLanguage language : FeatureHelpDisplayLanguage.values()) {
            result.put(language, load(language));
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> load(FeatureHelpDisplayLanguage language) {
        String resource = "/assets/chisetweaks/lang/" + language.resourceName() + ".json";
        try (InputStream input = FeatureHelpLanguageCatalog.class.getResourceAsStream(resource)) {
            if (input == null) return Map.of();
            byte[] bytes = input.readNBytes(MAX_LANGUAGE_BYTES + 1);
            if (bytes.length > MAX_LANGUAGE_BYTES) return Map.of();
            String json = StrictUtf8.decode(bytes);
            if (!StrictJsonSecurityPolicy.validateObjectDocument(json).valid()) return Map.of();
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    values.put(entry.getKey(), value.getAsString());
                }
            }
            return Map.copyOf(values);
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
    }
}
