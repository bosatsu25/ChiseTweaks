package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.LocalFeatureConfig;

/**
 * Configuration wrapper for lava highlight feature.
 */
public class LavaHighlightConfig {

    public boolean isEnabled() {
        return LocalFeatureConfig.getInstance().lavaHighlightEnabled;
    }

    public void setEnabled(boolean enabled) {
        LocalFeatureConfig.getInstance().lavaHighlightEnabled = enabled;
        LocalFeatureConfig.getInstance().save();
    }

    public boolean isHighlightSource() {
        return LocalFeatureConfig.getInstance().lavaHighlightSource;
    }

    public void setHighlightSource(boolean highlight) {
        LocalFeatureConfig.getInstance().lavaHighlightSource = highlight;
        LocalFeatureConfig.getInstance().save();
    }

    public boolean isHighlightFlowing() {
        return LocalFeatureConfig.getInstance().lavaHighlightFlowing;
    }

    public void setHighlightFlowing(boolean highlight) {
        LocalFeatureConfig.getInstance().lavaHighlightFlowing = highlight;
        LocalFeatureConfig.getInstance().save();
    }

    public int getSourceColor() {
        return LocalFeatureConfig.getInstance().lavaSourceColor;
    }

    public void setSourceColor(int color) {
        LocalFeatureConfig.getInstance().lavaSourceColor = color;
        LocalFeatureConfig.getInstance().save();
    }

    public int getFlowingColor() {
        return LocalFeatureConfig.getInstance().lavaFlowingColor;
    }

    public void setFlowingColor(int color) {
        LocalFeatureConfig.getInstance().lavaFlowingColor = color;
        LocalFeatureConfig.getInstance().save();
    }
}
