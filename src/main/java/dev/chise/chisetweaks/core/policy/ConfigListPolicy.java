package dev.chise.chisetweaks.core.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Normalizes bounded user lists used by Chise configuration. */
public final class ConfigListPolicy {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_ENTRY_CHARS = 256;

    private ConfigListPolicy() {}

    public static List<String> sanitize(List<String> input) {
        if (input == null) return List.of();
        ArrayList<String> output = new ArrayList<>(Math.min(input.size(), MAX_ENTRIES));
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < input.size() && output.size() < MAX_ENTRIES; index++) {
            String normalized = normalize(input.get(index));
            if (normalized != null && seen.add(normalized)) output.add(normalized);
        }
        return List.copyOf(output);
    }

    private static String normalize(String raw) {
        if (raw == null) return null;
        String value = raw.strip();
        if (value.isEmpty() || value.length() > MAX_ENTRY_CHARS) return null;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (Character.isISOControl(c) || c == '\u202A' || c == '\u202B'
                    || c == '\u202D' || c == '\u202E' || c == '\u2066'
                    || c == '\u2067' || c == '\u2068' || c == '\u2069') return null;
        }
        return value;
    }
}
