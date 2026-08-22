package dev.chise.chisetweaks.api.ore;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

 





public enum OreHighlightStyle {
    GENERIC("generic"),
    COAL("coal"),
    IRON("iron"),
    COPPER("copper"),
    GOLD("gold"),
    LAPIS("lapis"),
    REDSTONE("redstone"),
    DIAMOND("diamond"),
    EMERALD("emerald"),
    NETHER_GOLD("nether_gold"),
    QUARTZ("nether_quartz"),
    ANCIENT_DEBRIS("ancient_debris"),
    OBSIDIAN("obsidian"),
    CRYING_OBSIDIAN("crying_obsidian");

    private final String key;

    OreHighlightStyle(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public OreHighlightStyle next() {
        OreHighlightStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static @Nullable OreHighlightStyle fromKey(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (OreHighlightStyle style : values()) {
            if (style.key.equals(normalized)) return style;
        }
        return null;
    }
}
