package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

 




final class FeatureConfigDocumentPolicy {
    private FeatureConfigDocumentPolicy() {}

    static JsonObject sanitizeForRead(JsonObject source) {
        JsonObject sanitized = source == null ? new JsonObject() : source.deepCopy();
        JsonElement togglesElement = sanitized.get("FeatureToggles");
        if (togglesElement == null) return sanitized;
        if (!togglesElement.isJsonObject()) {
            sanitized.remove("FeatureToggles");
            return sanitized;
        }

        Set<String> known = new HashSet<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) known.add(toggle.getName());

        JsonObject toggles = togglesElement.getAsJsonObject();
        for (String key : Set.copyOf(toggles.keySet())) {
            JsonElement value = toggles.get(key);
            if (!known.contains(key) || value == null || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isBoolean()) {
                toggles.remove(key);
            }
        }
        return sanitized;
    }
}
