package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces client resource reloads needed when a model-backed visual feature changes state.
 *
 * <p>The model-loading callback runs only during resource reload. This coordinator compares the
 * desired config state with the state used for the last model load and requests at most one reload
 * at a time.</p>
 */
final class VisualModelReloadCoordinator {
    private static final AtomicBoolean RELOAD_IN_FLIGHT = new AtomicBoolean();
    private static volatile boolean appliedStateKnown;
    private static volatile boolean appliedDiamondModelState;

    private VisualModelReloadCoordinator() {}

    static void markAppliedDiamondModelState(boolean enabled) {
        appliedDiamondModelState = enabled;
        appliedStateKnown = true;
    }

    static void observe(Minecraft client, boolean desiredDiamondModelState) {
        if (client == null || !appliedStateKnown) return;
        if (desiredDiamondModelState == appliedDiamondModelState) return;
        if (!RELOAD_IN_FLIGHT.compareAndSet(false, true)) return;

        ChiseTweaksClient.LOGGER.info(
                "Refreshing client resources for Chise visual model state change: diamond={}",
                desiredDiamondModelState);
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                RELOAD_IN_FLIGHT.set(false);
                if (failure != null) {
                    ChiseTweaksClient.LOGGER.warn(
                            "Chise visual model resource reload failed after {}",
                            failure.getClass().getSimpleName());
                }
            });
        } catch (RuntimeException failure) {
            RELOAD_IN_FLIGHT.set(false);
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to start Chise visual model resource reload after {}",
                    failure.getClass().getSimpleName());
        }
    }
}
