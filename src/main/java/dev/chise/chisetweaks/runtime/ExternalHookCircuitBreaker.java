package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;

import java.util.concurrent.atomic.AtomicInteger;

/** Circuit breaker for the optional Sodium lava-render hook. */
public final class ExternalHookCircuitBreaker {
    private static final AtomicInteger OPEN_MASK = new AtomicInteger();
    private ExternalHookCircuitBreaker() {}
    public static boolean isOpen(Hook hook) { return (OPEN_MASK.get() & hook.bit()) != 0; }
    public static void trip(Hook hook, Throwable failure) {
        if (!FailureIsolationPolicy.isRecoverable(failure)) {
            if (failure instanceof Error error) throw error;
            throw (RuntimeException) failure;
        }
        int bit = hook.bit();
        int previous = OPEN_MASK.getAndUpdate(mask -> mask | bit);
        if ((previous & bit) == 0) ChiseTweaksClient.LOGGER.error(
                "Optional integration hook '{}' was disabled after {}", hook.diagnosticName(), failure.getClass().getSimpleName());
    }
    public static int openCount() { return Integer.bitCount(OPEN_MASK.get()); }
    public enum Hook {
        SODIUM_LAVA_HIGHLIGHT;
        private int bit() { return 1 << ordinal(); }
        private String diagnosticName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
