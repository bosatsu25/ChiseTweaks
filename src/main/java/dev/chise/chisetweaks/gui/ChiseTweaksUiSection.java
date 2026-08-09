package dev.chise.chisetweaks.gui;

import net.minecraft.network.chat.Component;

/** Focused top-level navigation for the retained Chise settings and guide screens. */
public enum ChiseTweaksUiSection {
    RESOURCES("Resources", "資源"),
    VISIBILITY("Visibility", "見やすさ"),
    HELP("Guide", "使い方");

    private final String english;
    private final String japanese;

    ChiseTweaksUiSection(String english, String japanese) {
        this.english = english;
        this.japanese = japanese;
    }

    public String getDisplayName() {
        return isJapanese() ? japanese : english;
    }

    public boolean isCategoryPage() {
        return this != HELP;
    }

    private static boolean isJapanese() {
        String probe = Component.translatable("screen.chisetweaks.help.language.probe").getString();
        return "ja".equalsIgnoreCase(probe);
    }
}
