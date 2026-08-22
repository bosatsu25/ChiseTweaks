package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

 
final class LocalFeatureConfigDocumentPolicy {
    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "lavaHighlightEnabled",
            "fireVisibilityEnabled",
            "oreHighlightAnimationEnabled",
            "worksiteVisibilityWorldOverlay",
            "worksiteVisibilityExclusiveMode");
    private static final Set<String> INTEGER_KEYS = Set.of(
            "worksiteVisibilityHorizontalRadius",
            "worksiteVisibilityVerticalRadius",
            "worksiteVisibilityIntervalTicks",
            "worksiteVisibilityMaxResults",
            "worksiteVisibilityMaxOverlayResults",
            "lavaAnalyzerHorizontalRadius",
            "lavaAnalyzerVerticalRadius",
            "lavaAnalyzerIntervalTicks",
            "lavaAnalyzerMaxOverlayResults",
            "visualTargetMask",
            "visualTargetSchemaVersion");

    private LocalFeatureConfigDocumentPolicy() {}

    static JsonObject overlayKnownValues(JsonObject defaults, JsonObject source) {
        JsonObject merged = defaults.deepCopy();
        if (source == null) return merged;

        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (BOOLEAN_KEYS.contains(key)) {
                if (!isBoolean(value)) throw new IllegalArgumentException("invalid boolean config field: " + key);
                merged.add(key, value.deepCopy());
            } else if (INTEGER_KEYS.contains(key)) {
                if (!isExactInt(value)) throw new IllegalArgumentException("invalid integer config field: " + key);
                merged.add(key, value.deepCopy());
            }
            // 削除済みまたは未知の設定キーは無視し、古い設定ファイルでも安全に縮退させる。
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
