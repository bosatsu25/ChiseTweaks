package dev.chise.chisetweaks.core.security;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Pure, bounded security rules used by local diagnostics and file-facing UI. */
public final class RuntimeSecurityPolicy {
    public static final int MAX_DIAGNOSTIC_VALUE_CHARS = 160;
    public static final long MAX_CONFIG_BYTES = 256L * 1024L;

    private static final Set<String> WINDOWS_RESERVED_LEAVES = Set.of(
            "CON", "PRN", "AUX", "NUL", "CLOCK$", "CONIN$", "CONOUT$",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private RuntimeSecurityPolicy() {
    }

    public static String sanitizeDiagnosticValue(String raw) {
        String value = Objects.requireNonNullElse(raw, "");
        StringBuilder result = new StringBuilder(Math.min(value.length(), MAX_DIAGNOSTIC_VALUE_CHARS));
        for (int index = 0; index < value.length() && result.length() < MAX_DIAGNOSTIC_VALUE_CHARS; index++) {
            char character = value.charAt(index);
            if (character == '$' && index + 1 < value.length() && value.charAt(index + 1) == '{') {
                appendBounded(result, '$');
                appendBounded(result, ' ');
            } else if (isUnsafeFormattingCharacter(character)) {
                appendBounded(result, ' ');
            } else {
                appendBounded(result, character);
            }
        }
        return result.toString().trim();
    }

    public static boolean isSafeLeafFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.length() > 128
                || !fileName.equals(fileName.strip())
                || !Normalizer.isNormalized(fileName, Normalizer.Form.NFC)
                || fileName.endsWith(".") || fileName.endsWith(" ")) {
            return false;
        }
        if (fileName.equals(".") || fileName.contains("..")
                || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains(":") || fileName.contains("*")
                || fileName.contains("?") || fileName.contains("\"")
                || fileName.contains("<") || fileName.contains(">")
                || fileName.contains("|")) {
            return false;
        }
        if (fileName.chars().anyMatch(value -> isUnsafeFormattingCharacter((char) value))
                || fileName.codePoints().anyMatch(RuntimeSecurityPolicy::isUnicodeNonCharacter)) {
            return false;
        }
        String baseName = fileName;
        int extension = baseName.indexOf('.');
        if (extension >= 0) {
            baseName = baseName.substring(0, extension);
        }
        return !WINDOWS_RESERVED_LEAVES.contains(baseName.toUpperCase(Locale.ROOT));
    }


    public static boolean isSafeJsonObjectKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128
                || !Normalizer.isNormalized(key, Normalizer.Form.NFC)) {
            return false;
        }
        return key.codePoints().noneMatch(codePoint ->
                Character.isISOControl(codePoint)
                        || isUnicodeNonCharacter(codePoint)
                        || isBidirectionalOverride(codePoint)
                        || codePoint == 0x200B
                        || codePoint == 0x200C
                        || codePoint == 0x200D
                        || codePoint == 0x2060
                        || codePoint == 0xFEFF);
    }


    private static void appendBounded(StringBuilder result, char character) {
        if (result.length() < MAX_DIAGNOSTIC_VALUE_CHARS) {
            result.append(character);
        }
    }

    private static boolean isUnsafeFormattingCharacter(char character) {
        return Character.isISOControl(character)
                || character == '\r'
                || character == '\n'
                || character == '\u2028'
                || character == '\u2029'
                || isBidirectionalOverride(character)
                || character == '\u200B'
                || character == '\u200C'
                || character == '\u200D'
                || character == '\uFEFF';
    }

    private static boolean isUnicodeNonCharacter(int codePoint) {
        return (codePoint >= 0xFDD0 && codePoint <= 0xFDEF)
                || (codePoint & 0xFFFF) == 0xFFFE
                || (codePoint & 0xFFFF) == 0xFFFF;
    }

    private static boolean isBidirectionalOverride(char character) {
        return isBidirectionalOverride((int) character);
    }

    private static boolean isBidirectionalOverride(int codePoint) {
        return (codePoint >= 0x202A && codePoint <= 0x202E)
                || (codePoint >= 0x2066 && codePoint <= 0x2069);
    }
}
