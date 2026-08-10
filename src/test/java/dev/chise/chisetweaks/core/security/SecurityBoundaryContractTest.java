package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SecurityBoundaryContractTest {
    @Test
    void strictJsonAcceptsNormalConfigShapesAndJsonScalarSyntax() {
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument("{}").valid());
        assertTrue(StrictJsonSecurityPolicy.validateObjectDocument("""
                {
                  "enabled": true,
                  "count": -12.5e+2,
                  "nothing": null,
                  "values": [false, 0, "text", {"nested":"ok"}],
                  "emoji": "\\uD83D\\uDE80"
                }
                """).valid());
    }

    @Test
    void strictJsonRejectsNonObjectDuplicateTrailingAndMalformedDocuments() {
        assertRejected(null);
        assertRejected("");
        assertRejected("[]");
        assertRejected("true");
        assertRejected("{} trailing");
        assertRejected("{\"a\":1,\"a\":2}");
        assertRejected("{a:1}");
        assertRejected("{\"a\" 1}");
        assertRejected("{\"a\":1 \"b\":2}");
        assertRejected("{\"a\":tru}");
        assertRejected("{\"a\":01}");
        assertRejected("{\"a\":1.}");
        assertRejected("{\"a\":1e}");
        assertRejected("{\"a\":--1}");
        assertRejected("{\"a\":\"\\q\"}");
        assertRejected("{\"a\":\"\\u12XZ\"}");
        assertRejected("{\"a\":\"\\uD83D\"}");
        assertRejected("{\"a\":\"\\uDE80\"}");
    }

    @Test
    void jsonStructureBudgetRejectsNulUnbalancedMismatchedAndOversizedStrings() {
        assertFalse(JsonStructureBudgetPolicy.validate("{\u0000}").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("}").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("{]").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("{").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("\"unterminated").valid());
        assertFalse(JsonStructureBudgetPolicy.validate("\"" + "a".repeat(
                JsonStructureBudgetPolicy.MAX_STRING_CHARS + 1) + "\"").valid());
        assertTrue(JsonStructureBudgetPolicy.validate("{\"a\":[1,2,3]}").valid());
    }

    @Test
    void jsonStructureBudgetEnforcesMaximumNesting() {
        String allowed = "[".repeat(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH)
                + "]".repeat(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH);
        String tooDeep = "[".repeat(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH + 1)
                + "]".repeat(JsonStructureBudgetPolicy.MAX_NESTING_DEPTH + 1);
        assertTrue(JsonStructureBudgetPolicy.validate(allowed).valid());
        assertFalse(JsonStructureBudgetPolicy.validate(tooDeep).valid());
    }

    @Test
    void runtimeSecuritySanitizesDiagnosticsWithoutExpandingThem() {
        assertEquals("hello world", RuntimeSecurityPolicy.sanitizeDiagnosticValue(" hello\nworld "));
        assertEquals("value $ {name}", RuntimeSecurityPolicy.sanitizeDiagnosticValue("value ${name}"));
        String longInput = "x".repeat(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS + 100);
        assertEquals(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS,
                RuntimeSecurityPolicy.sanitizeDiagnosticValue(longInput).length());
        assertEquals("", RuntimeSecurityPolicy.sanitizeDiagnosticValue(null));
    }

    @Test
    void runtimeSecurityAcceptsOnlySafePortableLeafFileNames() {
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("chisetweaks-visual.json"));
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(null));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(""));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(" config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("config.json "));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("../config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("a/b.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("a\\b.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("C:config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("CON"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("con.txt"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("NUL.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("config?.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("bad\nname.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("bad\u202Ename.json"));
    }

    @Test
    void runtimeSecurityRejectsUnsafeJsonObjectKeys() {
        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("FeatureToggles"));
        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("materialHighlights"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey(null));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey(""));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\nkey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u200Bkey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u202Ekey"));
    }

    private static void assertRejected(String json) {
        assertFalse(StrictJsonSecurityPolicy.validateObjectDocument(json).valid(),
                () -> "expected rejection for: " + json);
    }
}
