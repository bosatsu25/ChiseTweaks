package dev.chise.chisetweaks.core.performance;

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
