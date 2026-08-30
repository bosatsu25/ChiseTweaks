package dev.chise.chisetweaks.config;

import java.util.List;

public final class BuilderFocusConfig {
    public static final ChiseBooleanSetting REFRESH_RENDERER = new ChiseBooleanSetting(
            "refreshBuilderFocusRenderer",
            true);

    public static final ChiseRuleModeSetting BLOCK_RULE_MODE = new ChiseRuleModeSetting(
            "builderFocusBlockRuleMode", ChiseRuleMode.NONE, SettingPersistence.FEATURE_CONFIG);
    public static final ChiseStringListSetting BLOCK_WHITELIST = new ChiseStringListSetting(
            "builderFocusBlockWhitelist", List.of(), SettingPersistence.FEATURE_CONFIG);
    public static final ChiseStringListSetting BLOCK_BLACKLIST = new ChiseStringListSetting(
            "builderFocusBlockBlacklist", List.of(), SettingPersistence.FEATURE_CONFIG);

    public static final ChiseRuleModeSetting ENTITY_RULE_MODE = new ChiseRuleModeSetting(
            "builderFocusEntityRuleMode", ChiseRuleMode.NONE, SettingPersistence.FEATURE_CONFIG);
    public static final ChiseStringListSetting ENTITY_WHITELIST = new ChiseStringListSetting(
            "builderFocusEntityWhitelist", List.of(), SettingPersistence.FEATURE_CONFIG);
    public static final ChiseStringListSetting ENTITY_BLACKLIST = new ChiseStringListSetting(
            "builderFocusEntityBlacklist", List.of(), SettingPersistence.FEATURE_CONFIG);

    static final List<ChiseStringListSetting> STRING_LIST_OPTIONS = List.of(
            BLOCK_WHITELIST,
            BLOCK_BLACKLIST,
            ENTITY_WHITELIST,
            ENTITY_BLACKLIST);

    private BuilderFocusConfig() {}
}
