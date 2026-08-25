package dev.chise.chisetweaks.core.policy;

/** Warden Risk Analyzerのloaded-chunk探索・保持・描画上限。 */
public final class WardenRiskAnalyzerPolicy {
    public static final int DEFAULT_RANGE_BLOCKS = 48;
    public static final int MIN_RANGE_BLOCKS = 16;
    public static final int MAX_RANGE_BLOCKS = 64;

    public static final int DEFAULT_MAX_MARKERS = 64;
    public static final int MIN_MAX_MARKERS = 8;
    public static final int MAX_MAX_MARKERS = 64;

    public static final int MAX_BOOTSTRAP_CHUNK_RADIUS = 5;
    public static final int MAX_BOOTSTRAP_CHUNK_COUNT =
            (MAX_BOOTSTRAP_CHUNK_RADIUS * 2 + 1) * (MAX_BOOTSTRAP_CHUNK_RADIUS * 2 + 1);
    public static final int MAX_BOOTSTRAP_CHUNKS_PER_TICK = 16;
    public static final int MAX_VALIDATION_CHUNKS_PER_TICK = 4;
    public static final int MAX_TRACKED_CHUNKS = 256;
    public static final int MAX_DANGEROUS_SHRIEKERS_PER_CHUNK = 64;
    public static final int MAX_SENSOR_RESULTS = 128;
    public static final int VALIDATION_INTERVAL_TICKS = 20;
    public static final int ENTITY_CHECK_INTERVAL_TICKS = 10;
    public static final float ANALYZER_FACE_ALPHA_SCALE = 0.24f;

    private WardenRiskAnalyzerPolicy() {}

    public static int clampRangeBlocks(int value) {
        return Math.max(MIN_RANGE_BLOCKS, Math.min(MAX_RANGE_BLOCKS, value));
    }

    public static int clampMaxMarkers(int value) {
        return Math.max(MIN_MAX_MARKERS, Math.min(MAX_MAX_MARKERS, value));
    }

    public static int chunkRadiusForRangeBlocks(int rangeBlocks) {
        int range = clampRangeBlocks(rangeBlocks);
        return Math.min(MAX_BOOTSTRAP_CHUNK_RADIUS, (range + 15) / 16 + 1);
    }

    public static boolean isChunkRelevant(
            int centerChunkX,
            int centerChunkZ,
            int candidateChunkX,
            int candidateChunkZ,
            int rangeBlocks) {
        int radius = chunkRadiusForRangeBlocks(rangeBlocks);
        long deltaX = Math.abs((long) candidateChunkX - centerChunkX);
        long deltaZ = Math.abs((long) candidateChunkZ - centerChunkZ);
        return deltaX <= radius && deltaZ <= radius;
    }

    public static boolean withinRangeSquared(double distanceSquared, int rangeBlocks) {
        int range = clampRangeBlocks(rangeBlocks);
        return distanceSquared <= (double) range * range;
    }

    public static int colorForDistance(double distance) {
        if (distance <= 16.0) return 0xFFFF4D4D;
        if (distance <= 32.0) return 0xFFFF7A45;
        return 0xE6FFC247;
    }

    public static int fillColorForDistance(double distance) {
        int outline = colorForDistance(distance);
        int outlineAlpha = (outline >>> 24) & 0xFF;
        int fillAlpha = Math.round(outlineAlpha * ANALYZER_FACE_ALPHA_SCALE);
        return (fillAlpha << 24) | (outline & 0x00FFFFFF);
    }

    public static float edgeThicknessForDistance(double distance) {
        if (distance <= 16.0) return 0.038f;
        if (distance <= 32.0) return 0.030f;
        return 0.022f;
    }

    public static float boxInsetForDistance(double distance) {
        if (distance <= 32.0) return 0.018f;
        return 0.12f;
    }
}
