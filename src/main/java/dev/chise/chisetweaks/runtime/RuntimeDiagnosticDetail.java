package dev.chise.chisetweaks.runtime;

import java.util.Objects;
import java.util.regex.Pattern;

/** 診断ログへ追加するkey/valueを1行形式へ安全に正規化する。 */
public record RuntimeDiagnosticDetail(String key, String value) {
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,47}");
    private static final int MAX_VALUE_LENGTH = 128;

    public RuntimeDiagnosticDetail {
        key = Objects.requireNonNull(key, "key").trim();
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("diagnostic key has unsupported characters");
        }
        value = normalizeValue(value);
    }

    public static RuntimeDiagnosticDetail of(String key, Object value) {
        return new RuntimeDiagnosticDetail(key, String.valueOf(value));
    }

    public String toLogToken() {
        return key + "=" + value;
    }

    private static String normalizeValue(String raw) {
        String value = Objects.requireNonNull(raw, "value").trim();
        if (value.isEmpty()) return "none";
        StringBuilder normalized = new StringBuilder(Math.min(value.length(), MAX_VALUE_LENGTH));
        for (int index = 0; index < value.length() && normalized.length() < MAX_VALUE_LENGTH; index++) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current) || current == '=' || Character.isISOControl(current)) {
                normalized.append('_');
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }
}
