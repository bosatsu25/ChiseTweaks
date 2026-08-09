package dev.chise.chisetweaks.config;

import java.util.List;

/** Configuration owned by Scene Filter block/entity visibility. */
public final class BuilderFocusConfig {
    public static final SimpleBooleanSetting REFRESH_RENDERER = new SimpleBooleanSetting(
            "refreshBuilderFocusRenderer",
            true,
            "Refresh renderer",
            "描画を更新",
            "Reload visible chunks after Scene Filter block rules change.",
            "ブロック表示ルール変更後に表示中チャンクを更新します。");

    public static final ChiseRuleModeSetting BLOCK_RULE_MODE = new ChiseRuleModeSetting(
            "builderFocusBlockRuleMode", ChiseRuleMode.NONE);
    public static final ChiseStringListSetting BLOCK_WHITELIST = new ChiseStringListSetting(
            "builderFocusBlockWhitelist", List.of());
    public static final ChiseStringListSetting BLOCK_BLACKLIST = new ChiseStringListSetting(
            "builderFocusBlockBlacklist", List.of());

    public static final ChiseRuleModeSetting ENTITY_RULE_MODE = new ChiseRuleModeSetting(
            "builderFocusEntityRuleMode", ChiseRuleMode.NONE);
    public static final ChiseStringListSetting ENTITY_WHITELIST = new ChiseStringListSetting(
            "builderFocusEntityWhitelist", List.of());
    public static final ChiseStringListSetting ENTITY_BLACKLIST = new ChiseStringListSetting(
            "builderFocusEntityBlacklist", List.of());

    static final List<ChiseStringListSetting> STRING_LIST_OPTIONS = List.of(
            BLOCK_WHITELIST,
            BLOCK_BLACKLIST,
            ENTITY_WHITELIST,
            ENTITY_BLACKLIST);

    private BuilderFocusConfig() {}
}
