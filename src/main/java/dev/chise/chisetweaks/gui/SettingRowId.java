package dev.chise.chisetweaks.gui;

import java.util.Objects;

/** 設定画面rowの識別子を検証済みの値として保持する。 */
record SettingRowId(String value) {
    SettingRowId {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("setting row id must not be blank");
        if (!normalized.equals(value)) {
            throw new IllegalArgumentException("setting row id must not contain surrounding whitespace");
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            boolean valid = Character.isLetterOrDigit(current)
                    || current == '.'
                    || current == '_'
                    || current == '-';
            if (!valid) throw new IllegalArgumentException("setting row id contains an unsupported character");
        }
    }

    static SettingRowId of(String value) { return new SettingRowId(value); }

    boolean startsWith(String prefix) {
        return value.startsWith(Objects.requireNonNull(prefix, "prefix"));
    }

    @Override
    public String toString() { return value; }
}
