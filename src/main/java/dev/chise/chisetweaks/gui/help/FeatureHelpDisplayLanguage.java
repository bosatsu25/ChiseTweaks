package dev.chise.chisetweaks.gui.help;

/** Languages that can be selected locally inside the Chise feature guide. */
public enum FeatureHelpDisplayLanguage {
    JAPANESE("ja_jp"),
    ENGLISH("en_us");

    private final String resourceName;

    FeatureHelpDisplayLanguage(String resourceName) {
        this.resourceName = resourceName;
    }

    public String resourceName() {
        return resourceName;
    }
}
