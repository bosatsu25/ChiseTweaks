package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.performance.VisualModelReloadThrottlePolicy;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces client resource reloads needed when model-backed material targets change state.
 *
 * <p>The model-loading callback runs only during resource reload. Rapid target changes are
 * debounced into one reload, and failures use a bounded retry backoff instead of retrying every
 * client tick.</p>
 */
final class VisualModelReloadCoordinator {
    private static final AtomicBoolean RELOAD_IN_FLIGHT = new AtomicBoolean();
    private static final VisualModelReloadThrottlePolicy RELOAD_THROTTLE =
            new VisualModelReloadThrottlePolicy();
    private static volatile boolean appliedStateKnown;
    private static volatile int appliedMaterialModelMask;

    private VisualModelReloadCoordinator() {}

    static void markAppliedMaterialModelMask(int mask) {
        appliedMaterialModelMask = mask;
        appliedStateKnown = true;
    }

    static void observe(Minecraft client, int desiredMaterialModelMask) {
        if (client == null || !appliedStateKnown) return;
        if (!RELOAD_THROTTLE.shouldRequestReload(
                appliedMaterialModelMask,
                desiredMaterialModelMask,
                RELOAD_IN_FLIGHT.get())) {
            return;
        }
        if (!RELOAD_IN_FLIGHT.compareAndSet(false, true)) return;

        ChiseTweaksClient.LOGGER.info(
                "Refreshing client resources for Chise material model state change: 0x{} -> 0x{}",
                Integer.toHexString(appliedMaterialModelMask),
                Integer.toHexString(desiredMaterialModelMask));
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                if (failure == null) {
                    RELOAD_THROTTLE.onReloadSucceeded();
                } else {
                    RELOAD_THROTTLE.onReloadFailed();
                    ChiseTweaksClient.LOGGER.warn(
                            "Chise material model resource reload failed after {}",
                            failure.getClass().getSimpleName());
                }
                RELOAD_IN_FLIGHT.set(false);
            });
        } catch (RuntimeException failure) {
            RELOAD_THROTTLE.onReloadFailed();
            RELOAD_IN_FLIGHT.set(false);
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to start Chise material model resource reload after {}",
                    failure.getClass().getSimpleName());
        }
    }
}
