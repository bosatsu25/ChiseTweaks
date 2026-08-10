package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeSecurityPolicyTest {
    @Test
    void diagnosticSanitizerNeverSplitsSupplementaryCodePointsAtTheLengthBoundary() {
        String fitsExactly = "x".repeat(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS - 2) + "🚀";
        String wouldSplit = "x".repeat(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS - 1) + "🚀";

        String exact = RuntimeSecurityPolicy.sanitizeDiagnosticValue(fitsExactly);
        String truncated = RuntimeSecurityPolicy.sanitizeDiagnosticValue(wouldSplit);

        assertEquals(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS, exact.length());
        assertTrue(exact.endsWith("🚀"));
        assertEquals(RuntimeSecurityPolicy.MAX_DIAGNOSTIC_VALUE_CHARS - 1, truncated.length());
        assertWellFormedUtf16(exact);
        assertWellFormedUtf16(truncated);
    }

    @Test
    void diagnosticSanitizerNeutralizesLogFormattingAndInvisibleControlCharacters() {
        String input = "line1\nline2 ${name}\u202Ehidden\u200Btext";
        String sanitized = RuntimeSecurityPolicy.sanitizeDiagnosticValue(input);

        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("${"));
        assertFalse(sanitized.contains("\u202E"));
        assertFalse(sanitized.contains("\u200B"));
        assertTrue(sanitized.contains("$ {name}"));
    }

    @Test
    void safeLeafFilenameUsesInclusiveLengthBoundaryAndNfcNormalization() {
        String exactly128 = "a".repeat(123) + ".json";
        String tooLong = "a".repeat(124) + ".json";
        String decomposed = "e\u0301.json";
        String composed = Normalizer.normalize(decomposed, Normalizer.Form.NFC);

        assertEquals(128, exactly128.length());
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName(exactly128));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(tooLong));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName(decomposed));
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName(composed));
    }

    @Test
    void safeLeafFilenameRejectsWindowsDeviceNamesCaseInsensitivelyWithExtensions() {
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("CON"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("con.json"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("Lpt9.txt"));
        assertFalse(RuntimeSecurityPolicy.isSafeLeafFileName("clock$.json"));
        assertTrue(RuntimeSecurityPolicy.isSafeLeafFileName("console.json"));
    }

    @Test
    void jsonObjectKeysRejectVisualSpoofingAndWhitespaceWrappedVariants() {
        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("表示設定"));
        assertTrue(RuntimeSecurityPolicy.isSafeJsonObjectKey("a".repeat(128)));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("a".repeat(129)));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey(" FeatureToggles"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("FeatureToggles "));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u202Ekey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("bad\u200Bkey"));
        assertFalse(RuntimeSecurityPolicy.isSafeJsonObjectKey("e\u0301"));
    }

    @Test
    void diagnosticNoncharactersAreNeutralized() {
        String sanitized = RuntimeSecurityPolicy.sanitizeDiagnosticValue("a\uFDD0b");
        assertEquals("a b", sanitized);
    }

    private static void assertWellFormedUtf16(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                assertTrue(index + 1 < value.length());
                assertTrue(Character.isLowSurrogate(value.charAt(index + 1)));
                index++;
            } else {
                assertFalse(Character.isLowSurrogate(current));
            }
        }
    }
}
