package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OreHighlightResourceCompatibilityLoaderTest {
    @Test
    void validDocumentReturnsCompleteMapping() {
        Map<String, OreHighlightStyle> parsed = OreHighlightResourceCompatibilityLoader.parseDocument("""
                {"schemaVersion":1,"entries":[
                  {"block":"example:tin_ore","style":"iron"},
                  {"block":"example:silver_ore","style":"generic"}
                ]}
                """);

        assertEquals(2, parsed.size());
        assertEquals(OreHighlightStyle.IRON, parsed.get("example:tin_ore"));
        assertEquals(OreHighlightStyle.GENERIC, parsed.get("example:silver_ore"));
    }

    @Test
    void invalidLaterEntryRejectsWholeDocumentInsteadOfReturningPrefix() {
        String document = """
                {"schemaVersion":1,"entries":[
                  {"block":"example:tin_ore","style":"iron"},
                  {"block":"example:broken_ore","style":"not-a-style"}
                ]}
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> OreHighlightResourceCompatibilityLoader.parseDocument(document));
    }

    @Test
    void vanillaTargetIsRejectedBeforeAnyMappingIsReturned() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OreHighlightResourceCompatibilityLoader.parseDocument("""
                        {"schemaVersion":1,"entries":[
                          {"block":"minecraft:diamond_ore","style":"diamond"}
                        ]}
                        """));
    }

    @Test
    void compatibilityResourceRejectsTypeConfusion() {
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightResourceCompatibilityLoader.parseDocument("""
                        {"schemaVersion":"1","entries":[]}
                        """));
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightResourceCompatibilityLoader.parseDocument("""
                        {"schemaVersion":1,"entries":[{"block":123,"style":"iron"}]}
                        """));
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightResourceCompatibilityLoader.parseDocument("""
                        {"schemaVersion":1,"entries":[{"block":"example:tin_ore","style":1}]}
                        """));
    }

    @Test
    void compatibilityResourceRejectsNonIntegralSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightResourceCompatibilityLoader.parseDocument("""
                        {"schemaVersion":1.5,"entries":[]}
                        """));
    }
}
