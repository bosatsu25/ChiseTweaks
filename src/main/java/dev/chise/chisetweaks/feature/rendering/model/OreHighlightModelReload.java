package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

 










public final class OreHighlightModelReload {
    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final AtomicBoolean PENDING = new AtomicBoolean();

    private OreHighlightModelReload() {}

    public static void request() {
        
        
        if (!ChiseVisualModelPlugin.isModelPipelineReady()) return;
        if (!REQUESTED.compareAndSet(false, true)) {
            PENDING.set(true);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            resetAfterAbortedSchedule();
            return;
        }

        try {
            client.execute(() -> startReload(client));
        } catch (RuntimeException | LinkageError failure) {
            resetAfterAbortedSchedule();
            warnFailure(failure);
        }
    }

    private static void startReload(Minecraft client) {
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                try {
                    if (failure != null) warnFailure(failure);
                } finally {
                    completeReload();
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            resetAfterAbortedSchedule();
            warnFailure(failure);
        }
    }

    private static void completeReload() {
        REQUESTED.set(false);
        if (PENDING.getAndSet(false)) request();
    }

    private static void resetAfterAbortedSchedule() {
        REQUESTED.set(false);
        PENDING.set(false);
    }

    private static void warnFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Ore Highlight model reload failed after {}; keeping the current baked models",
                failure.getClass().getSimpleName());
    }
}
