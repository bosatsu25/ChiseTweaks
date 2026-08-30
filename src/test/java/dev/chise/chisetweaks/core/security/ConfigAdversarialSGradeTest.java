package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S6: adversarial JSON/config parser matrix. */
final class ConfigAdversarialSGradeTest {

    @Test
    void acceptsBoundedObjectDocuments() {
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument("{}").valid());
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument(
                "{\"enabled\":true,\"radius\":5,\"items\":[\"minecraft:blue_ice\"]}").valid());
    }

    @Test
    void rejectsMalformedAndNonObjectDocuments() {
        assertRejected("");
        assertRejected("[]");
        assertRejected("null");
        assertRejected("{\"a\":1");
        assertRejected("{\"a\":1} trailing");
        assertRejected("{\"a\":01}");
        assertRejected("{\"a\":1,}");
    }

    @Test
    void rejectsDuplicateAndUnicodeEquivalentKeys() {
        assertRejected("{\"enabled\":true,\"enabled\":false}");
        assertRejected("{\"é\":1,\"e\\u0301\":2}");
    }

    @Test
    void rejectsControlCharactersNulAndBrokenUnicode() {
        assertRejected("{\"a\":\"line\nfeed\"}");
        assertRejected("{\"a\":\"x\u0000y\"}");
        assertRejected("{\"a\":\"\\uD800\"}");
        assertRejected("{\"a\":\"\\uDC00\"}");
    }

    @Test
    void rejectsExcessiveNestingBeforeParserRecursionCanGrow() {
        String nested = "{}";
        for (int i = 0; i <= JsonStructureBudgetPolicy.MAX_NESTING_DEPTH; i++) {
            nested = "{\"x\":" + nested + "}";
        }
        assertRejected(nested);
    }

    @Test
    void rejectsOversizedStrings() {
        String payload = "x".repeat(JsonStructureBudgetPolicy.MAX_STRING_CHARS + 1);
        assertRejected("{\"value\":\"" + payload + "\"}");
    }

    @Test
    void rejectsObjectAndArrayCardinalityBombs() {
        StringBuilder object = new StringBuilder("{");
        for (int i = 0; i <= StrictJsonSecurityPolicy.MAX_OBJECT_MEMBERS; i++) {
            if (i != 0) object.append(',');
            object.append('\"').append('k').append(i).append("\":0");
        }
        object.append('}');
        assertRejected(object.toString());

        StringBuilder array = new StringBuilder("{\"a\":[");
        for (int i = 0; i <= StrictJsonSecurityPolicy.MAX_ARRAY_ELEMENTS; i++) {
            if (i != 0) array.append(',');
            array.append('0');
        }
        array.append("]}");
        assertRejected(array.toString());
    }

    @Test
    void structureBudgetRejectsMismatchedContainers() {
        assertFalse(JsonStructureBudgetPolicy.validate("{]").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("[}").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("}}").valid());
    }

    private static void assertRejected(String json) {
        StrictJsonSecurityPolicy.Validation validation =
                StrictJsonSecurityPolicy.validateObjectDocument(json);
        assertFalse(validation.valid(), () -> "Expected rejection: " + json);
        assertFalse(validation.reason().isBlank(), "Rejected input must expose a bounded diagnostic reason");
    }
}
