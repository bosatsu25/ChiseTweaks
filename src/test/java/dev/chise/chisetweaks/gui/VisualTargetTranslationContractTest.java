package dev.chise.chisetweaks.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualTargetTranslationContractTest {
    private static final Path LANGUAGE_DIRECTORY =
            Path.of("src/main/resources/assets/chisetweaks/lang");

    @Test
    void everyVisualTargetHasEnglishAndJapaneseNameAndDescription() throws IOException {
        JsonObject english = language("en_us");
        JsonObject japanese = language("ja_jp");

        for (ChiseBooleanSetting target : VisualTargetSettings.ALL_OPTIONS) {
            String base = "screen.chisetweaks.settings.target." + target.getName();
            assertTranslated(english, base + ".name", "en_us");
            assertTranslated(english, base + ".description", "en_us");
            assertTranslated(japanese, base + ".name", "ja_jp");
            assertTranslated(japanese, base + ".description", "ja_jp");
        }
    }

    @Test
    void technicalTargetLabelsUseProductFacingNames() throws IOException {
        JsonObject english = language("en_us");
        JsonObject japanese = language("ja_jp");

        assertEquals("Tripwire", text(english,
                "screen.chisetweaks.settings.target.visualTargetTechnicalTripwire.name"));
        assertEquals("Tripwire Hook", text(english,
                "screen.chisetweaks.settings.target.visualTargetTechnicalTripwireHook.name"));
        assertEquals("糸", text(japanese,
                "screen.chisetweaks.settings.target.visualTargetTechnicalTripwire.name"));
        assertEquals("トリップワイヤーフック", text(japanese,
                "screen.chisetweaks.settings.target.visualTargetTechnicalTripwireHook.name"));
    }

    private static JsonObject language(String locale) throws IOException {
        return JsonParser.parseString(Files.readString(LANGUAGE_DIRECTORY.resolve(locale + ".json")))
                .getAsJsonObject();
    }

    private static void assertTranslated(JsonObject language, String key, String locale) {
        assertTrue(language.has(key), () -> locale + " is missing " + key);
        assertTrue(language.get(key).isJsonPrimitive(), () -> locale + " must map text for " + key);
        assertTrue(!text(language, key).isBlank(), () -> locale + " has blank text for " + key);
    }

    private static String text(JsonObject language, String key) {
        return language.get(key).getAsString();
    }
}
