package dev.chise.chisetweaks.core.performance;

/**
 * Stateful debounce/backoff policy for resource reloads caused by model-backed target changes.
 *
 * <p>Rapid UI edits are coalesced into one reload after a short quiet period. A failed reload is
 * backed off instead of being retried every client tick.</p>
 */
public final class VisualModelReloadThrottlePolicy {
    public static final int QUIET_TICKS = 4;
    public static final int FAILURE_BACKOFF_TICKS = 100;

    private boolean desiredKnown;
    private int observedDesiredMask;
    private int stableTicks;
    private int retryCooldownTicks;

    public boolean shouldRequestReload(int appliedMask, int desiredMask, boolean reloadInFlight) {
        if (retryCooldownTicks > 0) retryCooldownTicks--;

        if (desiredMask == appliedMask) {
            desiredKnown = false;
            stableTicks = 0;
            return false;
        }

        if (!desiredKnown || desiredMask != observedDesiredMask) {
            desiredKnown = true;
            observedDesiredMask = desiredMask;
            stableTicks = 0;
            return false;
        }

        if (stableTicks < QUIET_TICKS) stableTicks++;
        return stableTicks >= QUIET_TICKS
                && retryCooldownTicks == 0
                && !reloadInFlight;
    }

    public void onReloadSucceeded() {
        retryCooldownTicks = 0;
        desiredKnown = false;
        stableTicks = 0;
    }

    public void onReloadFailed() {
        retryCooldownTicks = FAILURE_BACKOFF_TICKS;
    }

    public void reset() {
        desiredKnown = false;
        observedDesiredMask = 0;
        stableTicks = 0;
        retryCooldownTicks = 0;
    }

    public int retryCooldownTicks() {
        return retryCooldownTicks;
    }
}
