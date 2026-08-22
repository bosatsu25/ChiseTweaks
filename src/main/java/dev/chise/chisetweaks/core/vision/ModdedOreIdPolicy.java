package dev.chise.chisetweaks.core.vision;

import java.util.Locale;

public final class ModdedOreIdPolicy {
    private ModdedOreIdPolicy() {}

    public static boolean looksLikeOre(String rawId) {
        String id = normalize(rawId);
        int separator = id.indexOf(':');
        if (separator <= 0 || separator == id.length() - 1) return false;
        String namespace = id.substring(0, separator);
        String path = id.substring(separator + 1);
        if (namespace.equals("minecraft")) return false;
        return path.endsWith("_ore");
    }

    public static String normalize(String rawId) {
        return rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
    }
}
