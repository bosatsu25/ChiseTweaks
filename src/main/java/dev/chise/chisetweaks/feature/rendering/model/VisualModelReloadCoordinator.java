package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces client resource reloads needed when model-backed material targets change state.
 *
 * <p>The model-loading callback runs only during resource reload. This coordinator compares the
 * desired material target mask with the mask used for the last model load and requests at most one
 * reload at a time.</p>
 */
final class VisualModelReloadCoordinator {
    private static final AtomicBoolean RELOAD_IN_FLIGHT = new AtomicBoolean();
    private static volatile boolean appliedStateKnown;
    private static volatile int appliedMaterialModelMask;

    private VisualModelReloadCoordinator() {}

    static void markAppliedMaterialModelMask(int mask) {
        appliedMaterialModelMask = mask;
        appliedStateKnown = true;
    }

    static void observe(Minecraft client, int desiredMaterialModelMask) {
        if (client == null || !appliedStateKnown) return;
        if (desiredMaterialModelMask == appliedMaterialModelMask) return;
        if (!RELOAD_IN_FLIGHT.compareAndSet(false, true)) return;

        ChiseTweaksClient.LOGGER.info(
                "Refreshing client resources for Chise material model state change: 0x{} -> 0x{}",
                Integer.toHexString(appliedMaterialModelMask),
                Integer.toHexString(desiredMaterialModelMask));
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                RELOAD_IN_FLIGHT.set(false);
                if (failure != null) {
                    ChiseTweaksClient.LOGGER.warn(
                            "Chise material model resource reload failed after {}",
                            failure.getClass().getSimpleName());
                }
            });
        } catch (RuntimeException failure) {
            RELOAD_IN_FLIGHT.set(false);
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to start Chise material model resource reload after {}",
                    failure.getClass().getSimpleName());
        }
    }
}
