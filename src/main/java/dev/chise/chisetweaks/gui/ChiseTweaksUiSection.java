package dev.chise.chisetweaks.gui;

import fi.dy.masa.malilib.util.StringUtils;

/** Stable top-level navigation shared by the Chise settings and guide screens. */
public enum ChiseTweaksUiSection {
    PLACEMENT("Placement & Direction", "設置・向き"),
    RESOURCES("Resources", "資源"),
    VISIBILITY("Visibility", "見やすさ"),
    HOTKEYS("Keybinds", "キー設定"),
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
        return this == PLACEMENT || this == RESOURCES || this == VISIBILITY;
    }

    private static boolean isJapanese() {
        return "ja".equals(StringUtils.getTranslatedOrFallback(
                "screen.chisetweaks.help.language.probe", "en"));
    }
}
