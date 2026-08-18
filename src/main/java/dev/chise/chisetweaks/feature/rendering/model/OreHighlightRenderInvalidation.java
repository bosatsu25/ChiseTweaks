package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coalesces model-highlight setting changes into an on-demand client renderer rebuild.
 *
 * <p>No permanent tick callback is registered. A setting change schedules at most one client-thread
 * refresh. Transient renderer-transition failures are retried only a small bounded number of times
 * and are then abandoned rather than throwing into the client.</p>
 */
public final class OreHighlightRenderInvalidation {
    static final int MAX_FAILURE_RETRIES = 3;

    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final AtomicInteger FAILURE_RETRIES = new AtomicInteger();

    private OreHighlightRenderInvalidation() {}

    /** Requests a chunk-geometry refresh without starting a resource reload or permanent polling. */
    public static void request() {
        FAILURE_RETRIES.set(0);
        schedule();
    }

    private static void schedule() {
        if (!REQUESTED.compareAndSet(false, true)) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            REQUESTED.set(false);
            return;
        }
        try {
            client.execute(() -> refresh(client));
        } catch (RuntimeException | LinkageError failure) {
            REQUESTED.set(false);
            FAILURE_RETRIES.set(0);
            warnFailure(failure);
        }
    }

    private static void refresh(Minecraft client) {
        REQUESTED.set(false);
        if (client.level == null || client.levelRenderer == null) {
            FAILURE_RETRIES.set(0);
            return;
        }
        try {
            client.levelRenderer.allChanged();
            FAILURE_RETRIES.set(0);
        } catch (RuntimeException | LinkageError failure) {
            int retry = FAILURE_RETRIES.incrementAndGet();
            if (retry < MAX_FAILURE_RETRIES) {
                schedule();
            } else {
                FAILURE_RETRIES.set(0);
                warnFailure(failure);
            }
        }
    }

    private static void warnFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Model highlight renderer refresh failed after bounded retries: {}",
                failure.getClass().getSimpleName());
    }
}
