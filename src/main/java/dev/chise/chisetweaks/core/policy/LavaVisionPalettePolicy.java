package dev.chise.chisetweaks.core.policy;

/** Pure ARGB selection for source/flowing lava inspection. */
public final class LavaVisionPalettePolicy {
    public static final int NO_TINT = 0xFFFFFFFF;
    private LavaVisionPalettePolicy() {}

    public static int color(boolean enabled, boolean source, boolean showSource,
                            boolean showFlowing, int sourceRgb, int flowingRgb) {
        if (!enabled) return NO_TINT;
        if (source && showSource) return 0xFF000000 | sourceRgb;
        if (!source && showFlowing) return 0xFF000000 | flowingRgb;
        return NO_TINT;
    }
}
