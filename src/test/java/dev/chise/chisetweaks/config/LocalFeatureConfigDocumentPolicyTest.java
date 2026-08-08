package dev.chise.chisetweaks.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class LocalFeatureConfigDocumentPolicyTest {
    private static JsonObject defaults() {
        return JsonParser.parseString("""
                {
                  "lavaHighlightEnabled": false,
                  "lavaHighlightSource": true,
                  "lavaHighlightFlowing": true,
                  "lavaSourceColor": -50384,
                  "lavaFlowingColor": -27392,
                  "worksiteVisibilityHorizontalRadius": 5,
                  "worksiteVisibilityVerticalRadius": 3,
                  "worksiteVisibilityIntervalTicks": 10,
                  "worksiteVisibilityMaxResults": 6,
                  "worksiteVisibilityMaxOverlayResults": 12,
                  "worksiteVisibilityWorldOverlay": true,
                  "worksiteVisibilityExclusiveMode": false,
                  "visualTargetMask": 33554431,
                  "pumpkinScaffoldPlacementRange": 4
                }
                """).getAsJsonObject();
    }

    @Test
    void unknownKeysAreIgnoredForForwardCompatibility() {
        JsonObject source = JsonParser.parseString(
                "{\"futureOption\":true,\"pumpkinScaffoldPlacementRange\":5}").getAsJsonObject();

        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source);

        assertFalse(merged.has("futureOption"));
        assertEquals(5, merged.get("pumpkinScaffoldPlacementRange").getAsInt());
        assertEquals(10, merged.get("worksiteVisibilityIntervalTicks").getAsInt());
    }

    @Test
    void visualTargetMaskAcceptsExactIntegers() {
        JsonObject source = JsonParser.parseString("{\"visualTargetMask\":12345}").getAsJsonObject();
        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source);
        assertEquals(12345, merged.get("visualTargetMask").getAsInt());
    }

    @Test
    void stringEncodedNumbersAreRejected() {
        JsonObject source = JsonParser.parseString(
                "{\"pumpkinScaffoldPlacementRange\":\"5\"}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source));
    }

    @Test
    void fractionalNumbersAreRejectedForIntegerSettings() {
        JsonObject source = JsonParser.parseString(
                "{\"worksiteVisibilityIntervalTicks\":10.5}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source));
    }

    @Test
    void integerOverflowIsRejected() {
        JsonObject source = JsonParser.parseString(
                "{\"worksiteVisibilityIntervalTicks\":2147483648}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source));
    }

    @Test
    void nonBooleanValuesAreRejectedForBooleanSettings() {
        JsonObject source = JsonParser.parseString(
                "{\"worksiteVisibilityWorldOverlay\":1}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class,
                () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults(), source));
    }
}
