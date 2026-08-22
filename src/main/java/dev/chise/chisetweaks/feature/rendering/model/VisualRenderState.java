package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.jspecify.annotations.Nullable;

public final class VisualRenderState {
    private static volatile Snapshot current = Snapshot.disabled();

    private VisualRenderState() {}

    public static Snapshot current() {
        return current;
    }

    public static void refreshFromConfig() {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        current = new Snapshot(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue(),
                FeatureSwitches.GLASS_INSPECTION.getBooleanValue(),
                OreHighlightRuntimePolicy.motion(local.oreHighlightAnimationEnabled),
                VisualTargetSelectionPolicy.sanitizeMask(local.visualTargetMask));
    }

    public record Snapshot(
            boolean oreEnabled,
            boolean kelpEnabled,
            boolean glassEnabled,
            OreHighlightRuntimePolicy.Motion oreMotion,
            int visualTargetMask) {
        private static Snapshot disabled() {
            return new Snapshot(
                    false,
                    false,
                    false,
                    OreHighlightRuntimePolicy.Motion.STATIC,
                    VisualTargetSelectionPolicy.ALL_TARGETS_MASK);
        }

        public boolean shouldRenderOre(@Nullable Target target) {
            if (!oreEnabled) return false;
            return target == null || VisualTargetSelectionPolicy.isEnabled(visualTargetMask, target);
        }
    }
}
