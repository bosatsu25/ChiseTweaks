package dev.chise.chisetweaks.core.policy;

/** Pure visual semantics for Chise's Lava Analyzer. */
public final class LavaVisionPalettePolicy {
    /** Deep forest/signal green reserved for the nearest lava-source boundary. */
    public static final int SOURCE_OUTLINE_ARGB = 0xFF075B32;
    /** Darker member of the same semantic green family used at the analysis-range edge. */
    public static final int FAR_OUTLINE_ARGB = 0xFF021A0E;
    /** Distance at or inside which the selected #075B32 source colour is shown unchanged. */
    public static final double NEAR_DISTANCE_BLOCKS = 2.0;
    /** Fixed semantic distance used for the far end of the gradient, independent of UI radius changes. */
    public static final double FAR_DISTANCE_BLOCKS = 8.0;
    /** Slightly heavier than ordinary inspection lines for the direct source-guide path. */
    public static final float SOURCE_LINE_WIDTH = 3.4f;
    /** World-space edge thickness used by the through-terrain analyzer wireframe. */
    public static final float ANALYZER_EDGE_THICKNESS = 0.026f;

    /** Legacy no-tint value retained for old config/document compatibility tests. */
    public static final int NO_TINT = 0xFFFFFFFF;

    private LavaVisionPalettePolicy() {}

    /**
     * Returns whether one block should receive the source-analysis cube.
     *
     * <p>The analyzer is deliberately source-only. Flowing lava is left untouched, and fully
     * surrounded source blocks are skipped so dense lava volumes do not turn into a wall of boxes.</p>
     */
    public static boolean shouldHighlight(boolean enabled, boolean source, boolean exposed) {
        return enabled && source && exposed;
    }

    /**
     * Smoothly strengthens the reserved deep-green family as the player approaches a retained source.
     * The hue family stays distinct from emerald/copper ore highlights; only RGB intensity changes.
     */
    public static int colorForDistance(double distanceBlocks) {
        if (!Double.isFinite(distanceBlocks)) return FAR_OUTLINE_ARGB;
        double distance = Math.max(0.0, distanceBlocks);
        double t = 1.0 - clamp01((distance - NEAR_DISTANCE_BLOCKS)
                / (FAR_DISTANCE_BLOCKS - NEAR_DISTANCE_BLOCKS));
        return lerpOpaqueRgb(FAR_OUTLINE_ARGB, SOURCE_OUTLINE_ARGB, t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int lerpOpaqueRgb(int fromArgb, int toArgb, double t) {
        int fromR = (fromArgb >>> 16) & 0xFF;
        int fromG = (fromArgb >>> 8) & 0xFF;
        int fromB = fromArgb & 0xFF;
        int toR = (toArgb >>> 16) & 0xFF;
        int toG = (toArgb >>> 8) & 0xFF;
        int toB = toArgb & 0xFF;
        int r = interpolateChannel(fromR, toR, t);
        int g = interpolateChannel(fromG, toG, t);
        int b = interpolateChannel(fromB, toB, t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int interpolateChannel(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    /**
     * Legacy tint selector retained while old persisted fields are accepted by the config schema.
     * New runtime rendering does not use this tint path.
     */
    public static int color(boolean enabled, boolean source, boolean showSource,
                            boolean showFlowing, int sourceRgb, int flowingRgb) {
        if (!enabled) return NO_TINT;
        if (source && showSource) return 0xFF000000 | sourceRgb;
        if (!source && showFlowing) return 0xFF000000 | flowingRgb;
        return NO_TINT;
    }
}
