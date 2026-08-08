package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.security.JsonStructureBudgetPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class JsonStructureBudgetPolicyTest {
    @Test
    void acceptsBudgetsAtTheirBoundaries() {
        assertTrue(JsonStructureBudgetPolicy.validate(nestedArray(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH)).valid());
        assertTrue(JsonStructureBudgetPolicy.validate("\"" + "a".repeat(JsonStructureBudgetPolicy.MAX_STRING_CHARS) + "\"").valid());
        assertTrue(JsonStructureBudgetPolicy.validate(arrayWithElements(8_191)).valid());
    }

    @Test
    void rejectsNestingStringAndTokenBudgetsImmediatelyPastTheBoundary() {
        assertRejected(nestedArray(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH + 1), "nesting budget exceeded");
        assertRejected("\"" + "a".repeat(JsonStructureBudgetPolicy.MAX_STRING_CHARS + 1) + "\"", "string budget exceeded");
        assertRejected(arrayWithElements(8_192), "token budget exceeded");
    }

    @Test
    void rejectsStructurallyHostileInput() {
        assertRejected("[}", "mismatched closing token");
        assertRejected("]", "unbalanced closing token");
        assertRejected("[[", "unbalanced structure");
        assertRejected("\"unterminated", "unterminated string");
        assertRejected("\"escaped\\", "unterminated string");
        assertRejected("{\u0000}", "NUL character");
        assertRejected("\"line\nfeed\"", "unescaped control character");
    }

    private static void assertRejected(String json, String reason) {
        var validation = JsonStructureBudgetPolicy.validate(json);
        assertFalse(validation.valid());
        assertEquals(reason, validation.reason());
    }

    private static String nestedArray(int depth) {
        return "[".repeat(depth) + "0" + "]".repeat(depth);
    }

    private static String arrayWithElements(int elements) {
        StringBuilder json = new StringBuilder(elements * 2 + 2).append('[');
        for (int index = 0; index < elements; index++) {
            if (index > 0) json.append(',');
            json.append('0');
        }
        return json.append(']').toString();
    }
}
