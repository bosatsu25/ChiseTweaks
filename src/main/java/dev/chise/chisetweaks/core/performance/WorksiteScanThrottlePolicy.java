package dev.chise.chisetweaks.core.performance;

/**
 * Decides when the bounded worksite scanner should run.
 *
 * <p>Movement keeps the configured scan cadence, while a stationary client falls back to a
 * slower periodic refresh. Configuration/category changes are urgent so UI changes remain
 * responsive without forcing a full volume scan every tick.</p>
 */
public final class WorksiteScanThrottlePolicy {
    public static final int IDLE_INTERVAL_MULTIPLIER = 4;
    public static final int MIN_IDLE_INTERVAL_TICKS = 20;

    private WorksiteScanThrottlePolicy() {}

    public static int activeIntervalTicks(int requestedIntervalTicks) {
        return WorksiteVisibilityBudgetPolicy.clampIntervalTicks(requestedIntervalTicks);
    }

    public static int idleIntervalTicks(int requestedIntervalTicks) {
        int active = activeIntervalTicks(requestedIntervalTicks);
        int multiplied = Math.min(
                WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
                active * IDLE_INTERVAL_MULTIPLIER);
        return Math.max(MIN_IDLE_INTERVAL_TICKS, multiplied);
    }

    public static boolean shouldScan(
            int ticksSinceLastScan,
            int requestedIntervalTicks,
            boolean urgentChange,
            boolean movementObserved) {
        if (urgentChange) return true;
        int elapsed = Math.max(0, ticksSinceLastScan);
        int threshold = movementObserved
                ? activeIntervalTicks(requestedIntervalTicks)
                : idleIntervalTicks(requestedIntervalTicks);
        return elapsed >= threshold;
    }
}
