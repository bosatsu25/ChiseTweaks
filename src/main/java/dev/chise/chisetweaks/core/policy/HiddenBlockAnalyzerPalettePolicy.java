package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;

/** Hidden Block Analyzerのthrough-wall marker色を既存Highlight設定から決める純粋policy。 */
public final class HiddenBlockAnalyzerPalettePolicy {
    public static final double NEAR_DISTANCE_BLOCKS = 2.0;
    public static final double FAR_DISTANCE_BLOCKS = 8.0;
    public static final float ANALYZER_EDGE_THICKNESS = 0.026f;
    public static final float ANALYZER_BOX_INSET = 0.018f;

    private static final double FAR_BRIGHTNESS = 0.32;
    private static final double FACE_ALPHA_RATIO = 0.30;

    private HiddenBlockAnalyzerPalettePolicy() {}

    public static int colorForDistance(
            double distanceBlocks,
            int colorPreset,
            int opacityPercent) {
        int near = configuredOutline(colorPreset, opacityPercent);
        int far = scaleRgb(near, FAR_BRIGHTNESS);
        if (!Double.isFinite(distanceBlocks)) return far;
        double distance = Math.max(0.0, distanceBlocks);
        double t = 1.0 - clamp01((distance - NEAR_DISTANCE_BLOCKS)
                / (FAR_DISTANCE_BLOCKS - NEAR_DISTANCE_BLOCKS));
        return lerpArgb(far, near, t);
    }

    public static int fillColorForDistance(
            double distanceBlocks,
            int colorPreset,
            int opacityPercent) {
        int outline = colorForDistance(distanceBlocks, colorPreset, opacityPercent);
        int alpha = (int) Math.round(((outline >>> 24) & 0xFF) * FACE_ALPHA_RATIO);
        return withAlpha(outline, alpha);
    }

    private static int configuredOutline(int colorPreset, int opacityPercent) {
        VisualAssistanceStylePolicy.OverlayStyle base =
                new VisualAssistanceStylePolicy.OverlayStyle(
                        BlockInspectionCategory.HIDDEN_SURFACE.argb(),
                        90,
                        VisualAssistanceStylePolicy.Marker.CROSS);
        return WorksiteHighlightProfilePolicy.customize(
                base,
                BlockInspectionCategory.HIDDEN_SURFACE,
                WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET,
                WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT,
                colorPreset,
                opacityPercent,
                false,
                WorksiteHighlightProfilePolicy.DimensionProfile.OTHER).argb();
    }

    private static int scaleRgb(int argb, double scale) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = clampChannel((int) Math.round(((argb >>> 16) & 0xFF) * scale));
        int green = clampChannel((int) Math.round(((argb >>> 8) & 0xFF) * scale));
        int blue = clampChannel((int) Math.round((argb & 0xFF) * scale));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int lerpArgb(int fromArgb, int toArgb, double t) {
        int alpha = interpolateChannel((fromArgb >>> 24) & 0xFF, (toArgb >>> 24) & 0xFF, t);
        int red = interpolateChannel((fromArgb >>> 16) & 0xFF, (toArgb >>> 16) & 0xFF, t);
        int green = interpolateChannel((fromArgb >>> 8) & 0xFF, (toArgb >>> 8) & 0xFF, t);
        int blue = interpolateChannel(fromArgb & 0xFF, toArgb & 0xFF, t);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int withAlpha(int argb, int alpha) {
        return (clampChannel(alpha) << 24) | (argb & 0x00FFFFFF);
    }

    private static int interpolateChannel(int from, int to, double t) {
        return clampChannel((int) Math.round(from + (to - from) * t));
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
