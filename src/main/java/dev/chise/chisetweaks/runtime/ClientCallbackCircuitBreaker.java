package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Fixed-size circuit breaker for Chise-owned visual callbacks. */
public final class ClientCallbackCircuitBreaker {
    private static final AtomicInteger OPEN_MASK = new AtomicInteger();

    private ClientCallbackCircuitBreaker() {}

    public static boolean isOpen(Callback callback) {
        return (OPEN_MASK.get() & callback.bit()) != 0;
    }

    public static void run(Callback callback, Runnable action) {
        if (isOpen(callback)) return;
        try {
            action.run();
        } catch (RuntimeException | LinkageError failure) {
            trip(callback, failure);
        }
    }

    public static <T> void run(Callback callback, T value, Consumer<T> action) {
        if (isOpen(callback)) return;
        try {
            action.accept(value);
        } catch (RuntimeException | LinkageError failure) {
            trip(callback, failure);
        }
    }

    public static void trip(Callback callback, Throwable failure) {
        if (!FailureIsolationPolicy.isRecoverable(failure)) {
            if (failure instanceof Error error) throw error;
            throw (RuntimeException) failure;
        }
        int bit = callback.bit();
        int previous = OPEN_MASK.getAndUpdate(mask -> mask | bit);
        if ((previous & bit) == 0) {
            ChiseTweaksClient.LOGGER.error(
                    "Client callback '{}' was disabled after {}",
                    callback.diagnosticName(),
                    failure.getClass().getSimpleName());
        }
    }

    public static void resetSessionState() {
        OPEN_MASK.set(0);
    }

    public static int openCount() {
        return Integer.bitCount(OPEN_MASK.get());
    }

    public enum Callback {
        WORKSITE_VISIBILITY_WORLD_RENDER;

        private int bit() { return 1 << ordinal(); }
        private String diagnosticName() { return name().toLowerCase(Locale.ROOT); }
    }
}
