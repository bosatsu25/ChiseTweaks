package dev.chise.chisetweaks.core.policy;

public final class LavaVisionPalettePolicy {

    public static final int SOURCE_OUTLINE_ARGB = 0xFF075B32;

    public static final int FAR_OUTLINE_ARGB = 0xFF021A0E;

    public static final int ANALYZER_FACE_ALPHA = 0x4D;

    public static final double NEAR_DISTANCE_BLOCKS = 2.0;

    public static final double FAR_DISTANCE_BLOCKS = 8.0;

    public static final float ANALYZER_EDGE_THICKNESS = 0.026f;

    private LavaVisionPalettePolicy() {}

    public static boolean shouldHighlight(boolean enabled, boolean source, boolean boundary) {
        return enabled && source && boundary;
    }

    public static int colorForDistance(double distanceBlocks) {
        if (!Double.isFinite(distanceBlocks)) return FAR_OUTLINE_ARGB;
        double distance = Math.max(0.0, distanceBlocks);
        double t = 1.0 - clamp01((distance - NEAR_DISTANCE_BLOCKS)
                / (FAR_DISTANCE_BLOCKS - NEAR_DISTANCE_BLOCKS));
        return lerpOpaqueRgb(FAR_OUTLINE_ARGB, SOURCE_OUTLINE_ARGB, t);
    }

    public static int fillColorForDistance(double distanceBlocks) {
        return withAlpha(colorForDistance(distanceBlocks), ANALYZER_FACE_ALPHA);
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

    private static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    private static int interpolateChannel(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }
}
