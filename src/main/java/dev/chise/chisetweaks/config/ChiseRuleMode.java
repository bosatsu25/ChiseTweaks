package dev.chise.chisetweaks.config;

import java.util.Locale;

public enum ChiseRuleMode {
    NONE,
    BLACKLIST,
    WHITELIST;

    public static ChiseRuleMode parse(String raw) {
        if (raw == null) return NONE;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "blacklist" -> BLACKLIST;
            case "whitelist" -> WHITELIST;
            default -> NONE;
        };
    }

    public String storageName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
