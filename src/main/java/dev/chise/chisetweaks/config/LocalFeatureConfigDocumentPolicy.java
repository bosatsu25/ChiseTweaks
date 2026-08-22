package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.Map;

/** 現行のデフォルト設定を唯一のスキーマとして、安全な既知フィールドだけを読み込む。 */
final class LocalFeatureConfigDocumentPolicy {
    private LocalFeatureConfigDocumentPolicy() {}

    static JsonObject overlayKnownValues(JsonObject defaults, JsonObject source) {
        if (defaults == null) throw new IllegalArgumentException("defaults must not be null");
        JsonObject merged = defaults.deepCopy();
        if (source == null) return merged;

        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement expected = defaults.get(key);
            if (expected == null) continue;

            JsonElement value = entry.getValue();
            if (!expected.isJsonPrimitive()) {
                throw new IllegalArgumentException("unsupported config field type: " + key);
            }
            JsonPrimitive expectedPrimitive = expected.getAsJsonPrimitive();
            if (expectedPrimitive.isBoolean()) {
                if (!isBoolean(value)) {
                    throw new IllegalArgumentException("invalid boolean config field: " + key);
                }
            } else if (expectedPrimitive.isNumber()) {
                if (!isExactInt(value)) {
                    throw new IllegalArgumentException("invalid integer config field: " + key);
                }
            } else {
                throw new IllegalArgumentException("unsupported config field type: " + key);
            }
            merged.add(key, value.deepCopy());
        }
        return merged;
    }

    private static boolean isBoolean(JsonElement value) {
        return value != null
                && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean();
    }

    private static boolean isExactInt(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return false;
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isNumber()) return false;
        try {
            BigDecimal number = new BigDecimal(primitive.getAsString()).stripTrailingZeros();
            if (number.scale() > 0) return false;
            number.intValueExact();
            return true;
        } catch (ArithmeticException | NumberFormatException failure) {
            return false;
        }
    }
}
