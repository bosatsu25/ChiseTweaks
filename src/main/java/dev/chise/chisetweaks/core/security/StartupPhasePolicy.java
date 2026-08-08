package dev.chise.chisetweaks.core.security;

/** Fixed startup budget prevents a broken optional phase from cascading forever. */
public final class StartupPhasePolicy {
    public static final int MAX_RECORDED_FAILURES = 16;

    private StartupPhasePolicy() {
    }

    public static int boundedFailureCount(int requested) {
        return Math.max(0, Math.min(requested, MAX_RECORDED_FAILURES));
    }
}
