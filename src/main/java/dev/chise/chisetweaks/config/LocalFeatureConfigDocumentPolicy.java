package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Schema gate for the retained local JSON settings before Gson materializes them. */
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
            // Removed and unknown keys are ignored so old config files degrade safely.
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
