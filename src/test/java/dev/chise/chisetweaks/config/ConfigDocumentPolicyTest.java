package dev.chise.chisetweaks.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigDocumentPolicyTest {
    @Test
    void featureToggleDocumentKeepsOnlyKnownBooleanSwitches() {
        JsonObject root = new JsonObject();
        JsonObject toggles = new JsonObject();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggles.addProperty(toggle.getName(), true);
        toggles.addProperty("pumpkinScaffold", true);
        toggles.addProperty("placementGuide", true);
        toggles.addProperty("fineThreadTraceBadType", "true");
        toggles.addProperty(FeatureSwitches.FINE_THREAD_TRACE.getName(), "wrong-type");
        root.add("FeatureToggles", toggles);

        JsonObject sanitized = FeatureConfigDocumentPolicy.sanitizeForRead(root);
        JsonObject safeToggles = sanitized.getAsJsonObject("FeatureToggles");
        assertEquals(FeatureSwitches.VALUES.size() - 1, safeToggles.size());
        assertFalse(safeToggles.has("pumpkinScaffold"));
        assertFalse(safeToggles.has("placementGuide"));
        assertFalse(safeToggles.has("fineThreadTraceBadType"));
        assertFalse(safeToggles.has(FeatureSwitches.FINE_THREAD_TRACE.getName()));
        assertTrue(toggles.has("pumpkinScaffold"));
    }

    @Test
    void featureToggleDocumentHandlesNullMissingAndWrongSectionTypes() {
        assertEquals(0, FeatureConfigDocumentPolicy.sanitizeForRead(null).size());
        JsonObject unrelated = new JsonObject();
        unrelated.addProperty("Generic", true);
        JsonObject copied = FeatureConfigDocumentPolicy.sanitizeForRead(unrelated);
        assertNotSame(unrelated, copied);
        assertTrue(copied.has("Generic"));
        JsonObject wrongType = new JsonObject();
        wrongType.addProperty("FeatureToggles", true);
        assertFalse(FeatureConfigDocumentPolicy.sanitizeForRead(wrongType).has("FeatureToggles"));
    }

    @Test
    void localDocumentOverlaysOnlyCurrentBooleanAndIntegerFields() {
        JsonObject defaults = JsonParser.parseString("""
                {
                  "lavaHighlightEnabled": false,
                  "oreHighlightAnimationEnabled": false,
                  "worksiteVisibilityHorizontalRadius": 5,
                  "worksiteVisibilityVerticalRadius": 3,
                  "worksiteVisibilityIntervalTicks": 10,
                  "worksiteVisibilityMaxResults": 6,
                  "worksiteVisibilityMaxOverlayResults": 12,
                  "worksiteVisibilityWorldOverlay": true,
                  "worksiteVisibilityExclusiveMode": false,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """).getAsJsonObject();
        JsonObject source = JsonParser.parseString("""
                {
                  "lavaHighlightEnabled": true,
                  "oreHighlightAnimationEnabled": true,
                  "worksiteVisibilityHorizontalRadius": 8,
                  "worksiteVisibilityWorldOverlay": false,
                  "visualTargetMask": 123,
                  "pumpkinScaffoldPlacementRange": 5,
                  "lavaSourceColor": 1234
                }
                """).getAsJsonObject();

        JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults, source);
        assertTrue(merged.get("lavaHighlightEnabled").getAsBoolean());
        assertTrue(merged.get("oreHighlightAnimationEnabled").getAsBoolean());
        assertEquals(8, merged.get("worksiteVisibilityHorizontalRadius").getAsInt());
        assertFalse(merged.get("worksiteVisibilityWorldOverlay").getAsBoolean());
        assertEquals(123, merged.get("visualTargetMask").getAsInt());
        assertFalse(merged.has("pumpkinScaffoldPlacementRange"));
        assertFalse(merged.has("lavaSourceColor"));
        assertNotSame(defaults, merged);
    }

    @Test
    void localDocumentRejectsWrongPrimitiveTypesFractionsAndOverflow() {
        JsonObject defaults = JsonParser.parseString("""
                {"lavaHighlightEnabled":false,"oreHighlightAnimationEnabled":false,"worksiteVisibilityHorizontalRadius":5}
                """).getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"lavaHighlightEnabled\":\"true\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"oreHighlightAnimationEnabled\":1}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"worksiteVisibilityHorizontalRadius\":1.5}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"worksiteVisibilityHorizontalRadius\":\"8\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"worksiteVisibilityHorizontalRadius\":999999999999999999999}").getAsJsonObject()));
        JsonObject exactDecimal = LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                defaults, JsonParser.parseString("{\"worksiteVisibilityHorizontalRadius\":8.0}").getAsJsonObject());
        assertEquals(8, exactDecimal.get("worksiteVisibilityHorizontalRadius").getAsInt());
    }

    @Test
    void localDocumentNullSourceReturnsIndependentDefaultsCopy() {
        JsonObject defaults = JsonParser.parseString("{\"lavaHighlightEnabled\":false}").getAsJsonObject();
        JsonObject copy = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults, null);
        assertNotSame(defaults, copy);
        copy.addProperty("lavaHighlightEnabled", true);
        assertFalse(defaults.get("lavaHighlightEnabled").getAsBoolean());
    }

    @Test
    void localConfigMigrationKeepsSafeBoundsAndPreservesReleasedLavaSetting() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "lavaHighlightEnabled": true,
                  "worksiteVisibilityHorizontalRadius": 999,
                  "worksiteVisibilityVerticalRadius": -20,
                  "worksiteVisibilityIntervalTicks": 10000,
                  "worksiteVisibilityMaxResults": 999,
                  "worksiteVisibilityMaxOverlayResults": 999,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 1,
                  "pumpkinScaffoldPlacementRange": 5,
                  "lavaHighlightFlowing": true
                }
                """));
        assertTrue(config.lavaHighlightEnabled);
        assertFalse(config.oreHighlightAnimationEnabled);
        assertEquals(8, config.worksiteVisibilityHorizontalRadius);
        assertEquals(1, config.worksiteVisibilityVerticalRadius);
        assertEquals(100, config.worksiteVisibilityIntervalTicks);
        assertEquals(8, config.worksiteVisibilityMaxResults);
        assertEquals(24, config.worksiteVisibilityMaxOverlayResults);
        assertEquals(
                VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                        | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void localConfigSchemaTwoAddsTechnicalTargetsOnly() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));
        assertEquals(VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK, config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void currentSchemaPreservesExplicitlyDisabledTechnicalTargets() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """));
        assertEquals(0, config.visualTargetMask);
        assertEquals(VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION, config.visualTargetSchemaVersion);
    }

    @Test
    void localConfigPersistsExplicitOreAnimationOptIn() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "oreHighlightAnimationEnabled": true,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 3
                }
                """));
        assertTrue(config.oreHighlightAnimationEnabled);
        assertEquals(0, config.visualTargetMask);
    }

    @Test
    void localConfigRejectsUnsafeDocumentsAndRestoresDefaults() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        config.lavaHighlightEnabled = true;
        config.oreHighlightAnimationEnabled = true;
        config.worksiteVisibilityHorizontalRadius = 8;
        assertFalse(config.replaceFromJsonDocument("{\"lavaHighlightEnabled\":true,\"lavaHighlightEnabled\":false}"));
        assertFalse(config.lavaHighlightEnabled);
        assertFalse(config.oreHighlightAnimationEnabled);
        assertEquals(5, config.worksiteVisibilityHorizontalRadius);
        assertFalse(config.replaceFromJsonDocument("{\"lavaHighlightEnabled\":\"true\"}"));
        assertFalse(config.lavaHighlightEnabled);
        assertFalse(config.replaceFromJsonDocument(null));
        assertFalse(config.replaceFromJsonDocument("   "));
    }
}
