package dev.chise.chisetweaks.core.policy;

public final class AncientDebrisAnalyzerPolicy {
    public static final int DEFAULT_RANGE_BLOCKS = 64;
    public static final int MIN_RANGE_BLOCKS = 16;
    public static final int MAX_RANGE_BLOCKS = 256;

    public static final int DEFAULT_MAX_MARKERS = 64;
    public static final int MIN_MAX_MARKERS = 8;
    public static final int MAX_MAX_MARKERS = 128;

    public static final int MAX_BOOTSTRAP_CHUNK_RADIUS = 17;
    public static final int MAX_BOOTSTRAP_CHUNK_COUNT =
            (MAX_BOOTSTRAP_CHUNK_RADIUS * 2 + 1) * (MAX_BOOTSTRAP_CHUNK_RADIUS * 2 + 1);
    public static final int MAX_BOOTSTRAP_CHUNKS_PER_TICK = 64;
    public static final int MAX_VALIDATION_CHUNKS_PER_TICK = 16;
    public static final int MAX_TRACKED_CHUNKS = 4096;
    public static final int MAX_DEBRIS_PER_CHUNK = 256;
    public static final int VALIDATION_INTERVAL_TICKS = 20;
    public static final float ANALYZER_FACE_ALPHA_SCALE = 0.30f;

    private AncientDebrisAnalyzerPolicy() {}

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

    public static int bootstrapChunkCountForRangeBlocks(int rangeBlocks) {
        int radius = chunkRadiusForRangeBlocks(rangeBlocks);
        int width = radius * 2 + 1;
        return width * width;
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
        if (distance <= 16.0) return 0xFFF8D56B;
        if (distance <= 64.0) return 0xE6FFB347;
        if (distance <= 128.0) return 0xB3FF8C42;
        return 0x80FF6B35;
    }

    public static int fillColorForDistance(double distance) {
        int outline = colorForDistance(distance);
        int outlineAlpha = (outline >>> 24) & 0xFF;
        int fillAlpha = Math.round(outlineAlpha * ANALYZER_FACE_ALPHA_SCALE);
        return (fillAlpha << 24) | (outline & 0x00FFFFFF);
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
