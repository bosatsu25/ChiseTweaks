package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coalesces Ore Highlights setting changes into a client renderer rebuild without resource reloads.
 *
 * <p>Multiple UI changes collapse into one request. Renderer invalidation is fail-soft: a transient
 * failure during a world/renderer transition is retried only a small bounded number of times and is
 * then abandoned rather than throwing into the client tick loop. The next natural chunk rebuild or
 * a later user setting change will still use the current Ore Highlight state.</p>
 */
public final class OreHighlightRenderInvalidation {
    static final int MAX_FAILURE_RETRIES = 3;

    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final AtomicInteger FAILURE_RETRIES = new AtomicInteger();
    private static boolean registered;

    private OreHighlightRenderInvalidation() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!REQUESTED.getAndSet(false)) return;
            if (client == null || client.level == null || client.levelRenderer == null) {
                FAILURE_RETRIES.set(0);
                return;
            }
            try {
                client.levelRenderer.allChanged();
                FAILURE_RETRIES.set(0);
            } catch (RuntimeException failure) {
                int retry = FAILURE_RETRIES.incrementAndGet();
                if (retry < MAX_FAILURE_RETRIES) {
                    REQUESTED.set(true);
                } else {
                    FAILURE_RETRIES.set(0);
                    ChiseTweaksClient.LOGGER.warn(
                            "Ore Highlight renderer refresh failed after bounded retries: {}",
                            failure.getClass().getSimpleName());
                }
            }
        });
    }

    /** Requests a chunk-geometry refresh without starting a resource reload. */
    public static void request() {
        FAILURE_RETRIES.set(0);
        REQUESTED.set(true);
    }
}
