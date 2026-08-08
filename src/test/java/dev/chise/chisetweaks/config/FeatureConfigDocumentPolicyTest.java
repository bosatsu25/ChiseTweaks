package dev.chise.chisetweaks.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FeatureConfigDocumentPolicyTest {
    @Test
    void unknownAndMistypedFeatureTogglesAreRemovedWithoutDiscardingKnownBooleans() {
        JsonObject source = JsonParser.parseString("""
                {
                  "FeatureToggles": {
                    "pumpkinScaffold": true,
                    "glassInspection": false,
                    "futureFeature": true,
                    "materialHighlights": "yes"
                  },
                  "FutureSection": {"keep": 1}
                }
                """).getAsJsonObject();

        JsonObject sanitized = FeatureConfigDocumentPolicy.sanitizeForRead(source);
        JsonObject toggles = sanitized.getAsJsonObject("FeatureToggles");

        assertTrue(toggles.get("pumpkinScaffold").getAsBoolean());
        assertFalse(toggles.get("glassInspection").getAsBoolean());
        assertFalse(toggles.has("futureFeature"));
        assertFalse(toggles.has("materialHighlights"));
        assertTrue(sanitized.has("FutureSection"));
    }

    @Test
    void nonObjectToggleSectionIsDroppedInsteadOfBreakingTheRestOfTheDocument() {
        JsonObject source = JsonParser.parseString("""
                {"FeatureToggles": [true, false], "FeatureHotkeys": {}}
                """).getAsJsonObject();

        JsonObject sanitized = FeatureConfigDocumentPolicy.sanitizeForRead(source);

        assertFalse(sanitized.has("FeatureToggles"));
        assertTrue(sanitized.has("FeatureHotkeys"));
    }

    @Test
    void nullSourceProducesAnEmptySafeDocument() {
        JsonObject sanitized = FeatureConfigDocumentPolicy.sanitizeForRead(null);
        assertNotNull(sanitized);
        assertTrue(sanitized.entrySet().isEmpty());
    }
}
