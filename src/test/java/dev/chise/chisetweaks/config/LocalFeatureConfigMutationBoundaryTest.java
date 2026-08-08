package dev.chise.chisetweaks.config;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalFeatureConfigMutationBoundaryTest {
    private static JsonObject defaults() {
        JsonObject defaults = new JsonObject();
        defaults.addProperty("lavaHighlightEnabled", false);
        defaults.addProperty("lavaSourceColor", 7);
        return defaults;
    }

    @Test
    void nullSourceReturnsAnIndependentNonNullDefaultsCopy() {
        JsonObject defaults = defaults();
        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults, null);

        assertNotNull(merged);
        assertNotSame(defaults, merged);
        assertEquals(defaults, merged);
        merged.addProperty("lavaSourceColor", 9);
        assertEquals(7, defaults.get("lavaSourceColor").getAsInt());
    }

    @Test
    void knownBooleanAndExactIntegerValuesAreActuallyOverlaid() {
        JsonObject source = new JsonObject();
        source.addProperty("lavaHighlightEnabled", true);
        source.addProperty("lavaSourceColor", -2147483648);

        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source);
        assertTrue(merged.get("lavaHighlightEnabled").getAsBoolean());
        assertEquals(Integer.MIN_VALUE, merged.get("lavaSourceColor").getAsInt());

        JsonObject second = new JsonObject();
        second.addProperty("lavaHighlightEnabled", false);
        second.addProperty("lavaSourceColor", 2147483647);
        JsonObject max = LocalFeatureConfigDocumentPolicy.overlayKnownValues(merged, second);
        assertFalse(max.get("lavaHighlightEnabled").getAsBoolean());
        assertEquals(Integer.MAX_VALUE, max.get("lavaSourceColor").getAsInt());
    }

    @Test
    void jsonNullIsRejectedForBothKnownSchemaKinds() {
        JsonObject booleanNull = new JsonObject();
        booleanNull.add("lavaHighlightEnabled", JsonNull.INSTANCE);
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), booleanNull));

        JsonObject integerNull = new JsonObject();
        integerNull.add("lavaSourceColor", JsonNull.INSTANCE);
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), integerNull));
    }

    @Test
    void mathematicallyExactDecimalIntegersAreAccepted() {
        JsonObject source = new JsonObject();
        source.addProperty("lavaSourceColor", 1.0);
        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source);
        assertEquals(1, merged.get("lavaSourceColor").getAsInt());
    }
}
