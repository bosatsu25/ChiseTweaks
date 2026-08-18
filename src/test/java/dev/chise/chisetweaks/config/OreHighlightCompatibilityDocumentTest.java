package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OreHighlightCompatibilityDocumentTest {
    @Test
    void validDocumentRetainsExactTypedEntries() {
        List<OreHighlightCompatibilityConfig.Entry> parsed =
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":1,"entries":[
                          {"block":"example:tin_ore","style":"iron"}
                        ]}
                        """);

        assertEquals(List.of(
                new OreHighlightCompatibilityConfig.Entry("example:tin_ore", OreHighlightStyle.IRON)), parsed);
    }

    @Test
    void schemaVersionMustBeANumberNotANumericString() {
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":"1","entries":[]}
                        """));
    }

    @Test
    void blockAndStyleMustBeActualStrings() {
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":1,"entries":[{"block":123,"style":"iron"}]}
                        """));
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":1,"entries":[{"block":"example:tin_ore","style":1}]}
                        """));
    }

    @Test
    void fractionalAndOverflowingSchemaVersionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":1.5,"entries":[]}
                        """));
        assertThrows(IllegalArgumentException.class, () ->
                OreHighlightCompatibilityConfig.parseDocument("""
                        {"schemaVersion":999999999999999999999,"entries":[]}
                        """));
    }
}
