package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 設定変更後のchunk renderer再構築をcoalesceし、一過性失敗だけを固定回数retryする。 */
public final class ChunkRenderInvalidation {
    static final int MAX_FAILURE_RETRIES = 3;

    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final AtomicInteger FAILURE_RETRIES = new AtomicInteger();

    private ChunkRenderInvalidation() {}

    public static void request() {
        FAILURE_RETRIES.set(0);
        schedule();
    }

    private static void schedule() {
        if (!REQUESTED.compareAndSet(false, true)) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            REQUESTED.set(false);
            FAILURE_RETRIES.set(0);
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
                "Chunk renderer refresh failed after bounded retries: {}",
                failure.getClass().getSimpleName());
    }
}
