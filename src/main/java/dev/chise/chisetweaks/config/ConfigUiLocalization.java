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
    }

    private static void localize(Iterable<? extends IConfigBase> options) {
        for (IConfigBase option : options) {
            String base = "config.option." + option.getName().toLowerCase();
            option.setPrettyName(StringUtils.getTranslatedOrFallback(
                    base + ".name", StringUtils.splitCamelCase(option.getName())));
            option.setComment(StringUtils.getTranslatedOrFallback(
                    base + ".comment", option.getComment()));
        }
    }
}
