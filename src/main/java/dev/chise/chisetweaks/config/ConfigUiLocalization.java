package dev.chise.chisetweaks.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.StringUtils;

/** Applies localized action-oriented labels to the client feature configuration objects. */
public final class ConfigUiLocalization {
    private ConfigUiLocalization() {}

    public static void refresh() {
        localize(BuilderFocusConfig.GENERAL_OPTIONS);
        localize(BuilderFocusConfig.RULE_OPTIONS);

        LocalFeatureSettings.refreshTranslations();
        mirrorPrettyNamesToGui(LocalFeatureSettings.ALL_OPTIONS);

        VisualTargetSettings.refreshTranslations();
        mirrorPrettyNamesToGui(VisualTargetSettings.ALL_OPTIONS);
    }

    private static void localize(Iterable<? extends IConfigBase> options) {
        for (IConfigBase option : options) {
            String base = "config.option." + option.getName().toLowerCase();
            String displayName = StringUtils.getTranslatedOrFallback(
                    base + ".name", StringUtils.splitCamelCase(option.getName()));
            option.setPrettyName(displayName);
            option.setTranslatedName(displayName);
            option.setComment(StringUtils.getTranslatedOrFallback(
                    base + ".comment", option.getComment()));
        }
    }

    /**
     * MaLiLib 26.1 renders getTranslatedName() in config rows, while several legacy ConfigBase
     * adapters only updated prettyName. Mirror the already-localized pretty name so raw camelCase
     * config IDs never leak into Chise's settings UI.
     */
    private static void mirrorPrettyNamesToGui(Iterable<? extends IConfigBase> options) {
        for (IConfigBase option : options) {
            option.setTranslatedName(option.getPrettyName());
        }
    }
}
