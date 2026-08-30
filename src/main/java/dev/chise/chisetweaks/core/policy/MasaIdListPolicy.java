package dev.chise.chisetweaks.core.policy;

import java.util.List;

/** Deterministic NONE/WHITELIST/BLACKLIST policy shared by optional external-mod guards. */
public final class MasaIdListPolicy {
    public static final int NONE = 0;
    public static final int WHITELIST = 1;
    public static final int BLACKLIST = 2;

    private MasaIdListPolicy() {}

    public static boolean allows(String id, int mode, List<String> whitelist, List<String> blacklist) {
        String normalized = normalize(id);
        if (normalized.isEmpty()) return false;
        return switch (clampMode(mode)) {
            case WHITELIST -> contains(whitelist, normalized);
            case BLACKLIST -> !contains(blacklist, normalized);
            default -> true;
        };
    }

    public static int clampMode(int mode) {
        return mode < NONE || mode > BLACKLIST ? NONE : mode;
    }

    private static boolean contains(List<String> values, String expected) {
        if (values == null || values.isEmpty()) return false;
        for (String value : values) {
            if (expected.equals(normalize(value))) return true;
        }
        return false;
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
