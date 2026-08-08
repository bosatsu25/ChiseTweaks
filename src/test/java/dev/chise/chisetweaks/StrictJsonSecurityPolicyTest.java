package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.security.JsonStructureBudgetPolicy;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class StrictJsonSecurityPolicyTest {
    @Test
    void acceptsAValidBoundedObjectDocument() {
        var validation = StrictJsonSecurityPolicy.validateObjectDocument(
                "{\"enabled\":true,\"range\":4,\"names\":[\"stone\",\"glass\"],\"nested\":{\"value\":null}}");

        assertTrue(validation.valid(), validation.reason());
        assertEquals("", validation.reason());
    }

    @Test
    void rejectsNonObjectRootsAndTrailingContent() {
        assertRejected("[]", "root must be an object");
        assertRejected("true", "root must be an object");
        assertRejected("{} []", "trailing content");
    }

    @Test
    void rejectsMalformedJsonBeforeGsonCanSeeIt() {
        assertRejected("{", "unbalanced structure");
        assertRejected("{\"a\":}", "invalid number");
        assertRejected("{\"a\":01}", "leading zero");
        assertRejected("{\"a\":1.}", "invalid number fraction");
        assertRejected("{\"a\":1e+}", "invalid number exponent");
        assertRejected("{\"a\":tru}", "invalid literal");
        assertRejected("{\"a\":\"\\q\"}", "invalid escape sequence");
        assertRejected("{\"a\":\"unterminated}", "unterminated string");
        assertRejected("{\"a\":1,}", "object key must be a string");
        assertRejected("{\"a\":[1,]}", "invalid number");
    }

    @Test
    void rejectsDuplicateAndUnsafeObjectKeys() {
        assertRejected("{\"a\":1,\"a\":2}", "duplicate object key");
        assertRejected("{\"a\":1,\"\\u0061\":2}", "duplicate object key");
        assertRejected("{\"bad\\u202Ekey\":1}", "unsafe object key");
        assertRejected("{\"zero\\u200Bwidth\":1}", "unsafe object key");
    }

    @Test
    void rejectsInvalidSurrogatesAndControlCharacters() {
        assertRejected("{\"a\":\"\uD800\"}", "unpaired high surrogate");
        assertRejected("{\"a\":\"\uDC00\"}", "unpaired low surrogate");
        assertRejected("{\"a\":\"\\uD800x\"}", "unpaired escaped high surrogate");
        assertRejected("{\"a\":\"\\uDC00\"}", "unpaired escaped low surrogate");
        assertRejected("{\"a\":\"line\nfeed\"}", "unescaped control character");
    }

    @Test
    void enforcesObjectAndArrayCardinalityBudgets() {
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument(objectWithMembers(
                StrictJsonSecurityPolicy.MAX_OBJECT_MEMBERS)).valid());
        assertRejected(objectWithMembers(StrictJsonSecurityPolicy.MAX_OBJECT_MEMBERS + 1),
                "object member budget exceeded");

        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument(objectWithArrayElements(
                StrictJsonSecurityPolicy.MAX_ARRAY_ELEMENTS)).valid());
        assertRejected(objectWithArrayElements(StrictJsonSecurityPolicy.MAX_ARRAY_ELEMENTS + 1),
                "array element budget exceeded");
    }

    @Test
    void rejectsOversizedStringsAndExcessiveNesting() {
        String acceptedString = "a".repeat(JsonStructureBudgetPolicy.MAX_STRING_CHARS);
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument("{\"value\":\"" + acceptedString + "\"}").valid());

        String oversizedString = acceptedString + "a";
        assertRejected("{\"value\":\"" + oversizedString + "\"}", "string budget exceeded");

        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument(nestedObject(
                JsonStructureBudgetPolicy.MAX_NESTING_DEPTH)).valid());
        assertRejected(nestedObject(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH + 1),
                "nesting budget exceeded");
    }

    private static void assertRejected(String json, String reasonFragment) {
        var validation = StrictJsonSecurityPolicy.validateObjectDocument(json);
        assertFalse(validation.valid(), () -> "accepted hostile JSON: " + json);
        assertTrue(validation.reason().contains(reasonFragment),
                () -> "expected reason containing '" + reasonFragment + "' but was '" + validation.reason() + "'");
    }

    private static String objectWithMembers(int members) {
        StringBuilder json = new StringBuilder(members * 12).append('{');
        for (int index = 0; index < members; index++) {
            if (index > 0) json.append(',');
            json.append('"').append('k').append(index).append("\":0");
        }
        return json.append('}').toString();
    }

    private static String objectWithArrayElements(int elements) {
        StringBuilder json = new StringBuilder(elements * 2 + 16).append("{\"values\":[");
        for (int index = 0; index < elements; index++) {
            if (index > 0) json.append(',');
            json.append('0');
        }
        return json.append("]}").toString();
    }

    private static String nestedObject(int depth) {
        StringBuilder json = new StringBuilder(depth * 6 + 1);
        for (int index = 0; index < depth; index++) json.append("{\"a\":");
        json.append('0');
        for (int index = 0; index < depth; index++) json.append('}');
        return json.toString();
    }
}
