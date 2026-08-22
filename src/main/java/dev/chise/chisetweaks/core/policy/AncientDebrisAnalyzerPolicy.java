package dev.chise.chisetweaks.core.policy;

/** Pure bounds and visual LOD policy for the Nether-only Ancient Debris Analyzer. */
public final class AncientDebrisAnalyzerPolicy {
    public static final int DEFAULT_RANGE_BLOCKS = 64;
    public static final int MIN_RANGE_BLOCKS = 16;
    public static final int MAX_RANGE_BLOCKS = 256;

    public static final int DEFAULT_MAX_MARKERS = 64;
    public static final int MIN_MAX_MARKERS = 8;
    public static final int MAX_MAX_MARKERS = 128;

    public static final int MAX_TRACKED_CHUNKS = 4096;
    public static final int MAX_DEBRIS_PER_CHUNK = 256;
    public static final int VALIDATION_INTERVAL_TICKS = 20;

    private AncientDebrisAnalyzerPolicy() {}

    public static int clampRangeBlocks(int value) {
        return Math.max(MIN_RANGE_BLOCKS, Math.min(MAX_RANGE_BLOCKS, value));
    }

    public static int clampMaxMarkers(int value) {
        return Math.max(MIN_MAX_MARKERS, Math.min(MAX_MAX_MARKERS, value));
    }

    public static boolean withinRangeSquared(double distanceSquared, int rangeBlocks) {
        int range = clampRangeBlocks(rangeBlocks);
        return distanceSquared <= (double) range * range;
    }

    public static int colorForDistance(double distance) {
        if (distance <= 16.0) return 0xFFF8D56B;
        if (distance <= 64.0) return 0xE6FFB347;
        if (distance <= 128.0) return 0xB3FF8C42;
        return 0x80FF6B35;
    }

    public static float edgeThicknessForDistance(double distance) {
        if (distance <= 16.0) return 0.036f;
        if (distance <= 64.0) return 0.028f;
        if (distance <= 128.0) return 0.020f;
        return 0.014f;
    }

    public static float boxInsetForDistance(double distance) {
        if (distance <= 64.0) return 0.018f;
        if (distance <= 128.0) return 0.18f;
        return 0.32f;
    }
}
