package dev.chise.chisetweaks.core.policy;

/** Stable Vanilla range rules used by the infrastructure visibility overlays. */
public final class InfrastructureRangePolicy {
    public static final int LIGHTNING_ROD_RANGE = 128;
    public static final int MAX_BEACON_LEVEL = 4;
    public static final int DISCOVERY_HORIZONTAL_RADIUS = 16;
    public static final int DISCOVERY_VERTICAL_RADIUS = 8;
    public static final int MAX_TARGETS = 8;
    public static final int SCAN_INTERVAL_TICKS = 20;

    private InfrastructureRangePolicy() {}

    public static int beaconRadius(int level) {
        int normalized = Math.max(0, Math.min(MAX_BEACON_LEVEL, level));
        return normalized == 0 ? 0 : normalized * 10 + 10;
    }
}
