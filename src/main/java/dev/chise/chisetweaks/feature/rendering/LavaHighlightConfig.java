package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;

/** Minimal persisted switch for Lava Analyzer. Visual semantics are owned by LavaVisionPalettePolicy. */
public final class LavaHighlightConfig {
    public boolean isEnabled() {
        return PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)
                && LocalFeatureConfig.getInstance().lavaHighlightEnabled;
    }

    public void setEnabled(boolean enabled) {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.lavaHighlightEnabled = PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)
                && enabled;
        config.save();
    }
}
