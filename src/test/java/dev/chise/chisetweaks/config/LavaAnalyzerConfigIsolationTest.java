package dev.chise.chisetweaks.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LavaAnalyzerConfigIsolationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void oldSharedScanValuesSeedDedicatedLavaSettingsDuringMigration() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityHorizontalRadius": 7,
                  "worksiteVisibilityVerticalRadius": 4,
                  "worksiteVisibilityIntervalTicks": 35,
                  "worksiteVisibilityMaxOverlayResults": 19,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertEquals(7, config.lavaAnalyzerHorizontalRadius);
        assertEquals(4, config.lavaAnalyzerVerticalRadius);
        assertEquals(35, config.lavaAnalyzerIntervalTicks);
        assertEquals(19, config.lavaAnalyzerMaxOverlayResults);
    }

    @Test
    void explicitLavaSettingsRemainIndependentFromHighlightScanSettings() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "worksiteVisibilityHorizontalRadius": 2,
                  "worksiteVisibilityVerticalRadius": 1,
                  "worksiteVisibilityIntervalTicks": 15,
                  "worksiteVisibilityMaxOverlayResults": 5,
                  "lavaAnalyzerHorizontalRadius": 8,
                  "lavaAnalyzerVerticalRadius": 5,
                  "lavaAnalyzerIntervalTicks": 60,
                  "lavaAnalyzerMaxOverlayResults": 24,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertEquals(2, config.worksiteVisibilityHorizontalRadius);
        assertEquals(1, config.worksiteVisibilityVerticalRadius);
        assertEquals(15, config.worksiteVisibilityIntervalTicks);
        assertEquals(5, config.worksiteVisibilityMaxOverlayResults);
        assertEquals(8, config.lavaAnalyzerHorizontalRadius);
        assertEquals(5, config.lavaAnalyzerVerticalRadius);
        assertEquals(60, config.lavaAnalyzerIntervalTicks);
        assertEquals(24, config.lavaAnalyzerMaxOverlayResults);
    }

    @Test
    void dedicatedLavaSettingsUseTheSameBoundedSafetyPolicy() {
        LocalFeatureConfig config = new LocalFeatureConfig();
        assertTrue(config.replaceFromJsonDocument("""
                {
                  "lavaAnalyzerHorizontalRadius": 999,
                  "lavaAnalyzerVerticalRadius": -50,
                  "lavaAnalyzerIntervalTicks": 9999,
                  "lavaAnalyzerMaxOverlayResults": 999,
                  "visualTargetMask": 0,
                  "visualTargetSchemaVersion": 2
                }
                """));

        assertEquals(8, config.lavaAnalyzerHorizontalRadius);
        assertEquals(1, config.lavaAnalyzerVerticalRadius);
        assertEquals(100, config.lavaAnalyzerIntervalTicks);
        assertEquals(24, config.lavaAnalyzerMaxOverlayResults);
    }

    @Test
    void documentPolicyRejectsWrongTypesForDedicatedLavaSettings() {
        JsonObject defaults = JsonParser.parseString("""
                {"lavaAnalyzerHorizontalRadius":5,"lavaAnalyzerIntervalTicks":10}
                """).getAsJsonObject();

        assertThrows(IllegalArgumentException.class, () ->
                LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                        defaults,
                        JsonParser.parseString("{\"lavaAnalyzerHorizontalRadius\":\"8\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () ->
                LocalFeatureConfigDocumentPolicy.overlayKnownValues(
                        defaults,
                        JsonParser.parseString("{\"lavaAnalyzerIntervalTicks\":1.5}").getAsJsonObject()));
    }

    @Test
    void lavaRuntimeReadsOnlyDedicatedLavaBudgetFields() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"));

        assertTrue(source.contains("local.lavaAnalyzerHorizontalRadius"));
        assertTrue(source.contains("local.lavaAnalyzerVerticalRadius"));
        assertTrue(source.contains("local.lavaAnalyzerIntervalTicks"));
        assertTrue(source.contains("local.lavaAnalyzerMaxOverlayResults"));
        assertFalse(source.contains("local.worksiteVisibilityHorizontalRadius"));
        assertFalse(source.contains("local.worksiteVisibilityVerticalRadius"));
        assertFalse(source.contains("local.worksiteVisibilityIntervalTicks"));
        assertFalse(source.contains("local.worksiteVisibilityMaxOverlayResults"));
    }
}
