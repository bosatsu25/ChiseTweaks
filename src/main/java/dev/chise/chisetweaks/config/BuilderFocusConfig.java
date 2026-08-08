package dev.chise.chisetweaks.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;

import java.util.List;

/**
 * Configuration owned by Scene Filter block/entity visibility.
 *
 * <p>Keeping these options beside the feature domain avoids a catch-all config catalogue and
 * makes it clear which runtime component consumes each setting.</p>
 */
public final class BuilderFocusConfig {
    public static final ConfigBoolean REFRESH_RENDERER = new ConfigBoolean(
            "refreshBuilderFocusRenderer", true,
            "Reload visible chunks after Scene Filter block rules change.");

    public static final ConfigOptionList BLOCK_RULE_MODE = new ConfigOptionList(
            "builderFocusBlockRuleMode", UsageRestriction.ListType.NONE,
            "How Scene Filter: Blocks applies block visibility rules.");
    public static final ConfigStringList BLOCK_WHITELIST = new ConfigStringList(
            "builderFocusBlockWhitelist", ImmutableList.copyOf(List.<String>of()),
            "Blocks kept visible by Scene Filter: Blocks.");
    public static final ConfigStringList BLOCK_BLACKLIST = new ConfigStringList(
            "builderFocusBlockBlacklist", ImmutableList.copyOf(List.<String>of()),
            "Blocks hidden by Scene Filter: Blocks.");

    public static final ConfigOptionList ENTITY_RULE_MODE = new ConfigOptionList(
            "builderFocusEntityRuleMode", UsageRestriction.ListType.NONE,
            "How Scene Filter: Entities applies entity visibility rules.");
    public static final ConfigStringList ENTITY_WHITELIST = new ConfigStringList(
            "builderFocusEntityWhitelist", ImmutableList.copyOf(List.<String>of()),
            "Entity types kept visible by Scene Filter: Entities.");
    public static final ConfigStringList ENTITY_BLACKLIST = new ConfigStringList(
            "builderFocusEntityBlacklist", ImmutableList.copyOf(List.<String>of()),
            "Entity types hidden by Scene Filter: Entities.");

    public static final List<IConfigBase> GENERAL_OPTIONS = List.of(REFRESH_RENDERER);
    public static final List<IConfigBase> RULE_OPTIONS = List.of(
            BLOCK_RULE_MODE,
            BLOCK_WHITELIST,
            BLOCK_BLACKLIST,
            ENTITY_RULE_MODE,
            ENTITY_WHITELIST,
            ENTITY_BLACKLIST);

    static final List<ConfigStringList> STRING_LIST_OPTIONS = List.of(
            BLOCK_WHITELIST,
            BLOCK_BLACKLIST,
            ENTITY_WHITELIST,
            ENTITY_BLACKLIST);

    private BuilderFocusConfig() {}
}
