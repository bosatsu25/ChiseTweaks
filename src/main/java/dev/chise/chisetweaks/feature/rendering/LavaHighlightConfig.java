package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.LocalFeatureConfig;

/** Minimal persisted switch for Lava Analyzer. Visual semantics are owned by LavaVisionPalettePolicy. */
public final class LavaHighlightConfig {
    public boolean isEnabled() {
        return LocalFeatureConfig.getInstance().lavaHighlightEnabled;
    }

    public void setEnabled(boolean enabled) {
        LocalFeatureConfig.getInstance().lavaHighlightEnabled = enabled;
        LocalFeatureConfig.getInstance().save();
    }
}
