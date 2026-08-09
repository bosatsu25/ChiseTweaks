package dev.chise.chisetweaks.core.performance;

/** Fixed budgets for local visibility scans. No scan may load chunks or run every frame. */
public final class WorksiteVisibilityBudgetPolicy {
    public static final int MIN_HORIZONTAL_RADIUS = 1;
    public static final int MAX_HORIZONTAL_RADIUS = 8;
    public static final int MIN_VERTICAL_RADIUS = 1;
    public static final int MAX_VERTICAL_RADIUS = 5;
    public static final int MIN_INTERVAL_TICKS = 5;
    public static final int MAX_INTERVAL_TICKS = 100;
    public static final int MAX_SCAN_CANDIDATES = 128;
    public static final int MAX_OVERLAY_RESULTS = 24;

    /**
     * Hard CPU/allocation budget for ray based line-of-sight checks in one scan.
     *
     * <p>Each clip query creates short-lived Minecraft geometry/context objects. Capping the
     * number of rays therefore bounds both main-thread work and scan-triggered allocation spikes,
     * even when many high-priority candidates are hidden behind terrain.</p>
     */
    public static final int MAX_LINE_OF_SIGHT_RAYS_PER_SCAN = 192;

    public static final int MAX_LOADED_CHUNK_PROBES = maximumLoadedChunkProbesFor(MAX_HORIZONTAL_RADIUS);

    private WorksiteVisibilityBudgetPolicy() {
    }

    public static int clampHorizontalRadius(int requested) {
        return clamp(requested, MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS);
    }

    public static int clampVerticalRadius(int requested) {
        return clamp(requested, MIN_VERTICAL_RADIUS, MAX_VERTICAL_RADIUS);
    }

    public static int clampIntervalTicks(int requested) {
        return clamp(requested, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
    }

    public static int clampOverlayResults(int requested) {
        return clamp(requested, 1, MAX_OVERLAY_RESULTS);
    }

    public static int maximumBlocksFor(int horizontalRadius, int verticalRadius) {
        int horizontal = clampHorizontalRadius(horizontalRadius);
        int vertical = clampVerticalRadius(verticalRadius);
        return (horizontal * 2 + 1) * (horizontal * 2 + 1) * (vertical * 2 + 1);
    }

    /**
     * Worst-case loaded-chunk probes needed to cover the horizontal scan square.
     *
     * <p>The scanner checks each intersected chunk once, then reuses that result for every block
     * column and Y level. With the current radius cap this is at most four chunk probes per scan,
     * instead of one chunk-source lookup per candidate block.</p>
     */
    public static int maximumLoadedChunkProbesFor(int horizontalRadius) {
        int horizontal = clampHorizontalRadius(horizontalRadius);
        int width = horizontal * 2 + 1;
        int chunksPerAxis = (width + 30) / 16;
        return chunksPerAxis * chunksPerAxis;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
