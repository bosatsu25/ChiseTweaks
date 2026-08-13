package dev.chise.chisetweaks.feature.rendering.model;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces Ore Highlights setting changes into one client renderer rebuild on the next tick.
 *
 * <p>This deliberately invalidates only rendered chunk geometry. It never reloads resource packs,
 * model assets, shader packs, or server state. Multiple UI changes in the same tick collapse into
 * one rebuild, and changes made while no world is open need no rebuild because the next world will
 * compile chunks from the current settings.</p>
 */
public final class OreHighlightRenderInvalidation {
    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static boolean registered;

    private OreHighlightRenderInvalidation() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!REQUESTED.getAndSet(false)) return;
            if (client == null || client.level == null || client.levelRenderer == null) return;
            client.levelRenderer.allChanged();
        });
    }

    /** Requests a chunk-geometry refresh without starting a resource reload. */
    public static void request() {
        REQUESTED.set(true);
    }
}
