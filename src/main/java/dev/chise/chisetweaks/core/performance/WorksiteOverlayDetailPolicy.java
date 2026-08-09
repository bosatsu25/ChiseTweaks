package dev.chise.chisetweaks.core.performance;

/**
 * Bounds per-frame line-geometry cost for distant worksite markers.
 *
 * <p>Near targets keep the full category-specific geometry. Distant targets use a compact
 * representation so the maximum overlay count cannot multiply the most expensive lattice or
 * bracket geometry across the whole scan radius.</p>
 */
public final class WorksiteOverlayDetailPolicy {
    public static final double FULL_DETAIL_DISTANCE_SQUARED = 49.0;

    private WorksiteOverlayDetailPolicy() {}

    public static Detail detailFor(double distanceSquared) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0) {
            return Detail.COMPACT;
        }
        return distanceSquared <= FULL_DETAIL_DISTANCE_SQUARED
                ? Detail.FULL
                : Detail.COMPACT;
    }

    public enum Detail {
        FULL,
        COMPACT
    }
}
