package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces rare compatibility changes into a model/resource reload.
 *
 * <p>Ore target discovery is intentionally a cold-path operation. Normal feature toggles still use
 * {@link OreHighlightRenderInvalidation}; only changes that can alter which block models need a
 * Chise wrapper request this reload.</p>
 */
public final class OreHighlightModelReload {
    private static final AtomicBoolean REQUESTED = new AtomicBoolean();

    private OreHighlightModelReload() {}

    public static void request() {
        // Registrations performed during client initialization are naturally included in the first
        // model bake and must not cause a redundant startup reload.
        if (!ChiseVisualModelPlugin.isModelPipelineReady()) return;
        if (!REQUESTED.compareAndSet(false, true)) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            REQUESTED.set(false);
            return;
        }

        try {
            client.execute(() -> startReload(client));
        } catch (RuntimeException | LinkageError failure) {
            REQUESTED.set(false);
            warnFailure(failure);
        }
    }

    private static void startReload(Minecraft client) {
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                try {
                    if (failure != null) warnFailure(failure);
                } finally {
                    REQUESTED.set(false);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            REQUESTED.set(false);
            warnFailure(failure);
        }
    }

    private static void warnFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Ore Highlight model reload failed after {}; keeping the current baked models",
                failure.getClass().getSimpleName());
    }
}
