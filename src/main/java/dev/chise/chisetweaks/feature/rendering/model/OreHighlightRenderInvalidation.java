package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

 






public final class OreHighlightRenderInvalidation {
    static final int MAX_FAILURE_RETRIES = 3;

    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final AtomicInteger FAILURE_RETRIES = new AtomicInteger();

    private OreHighlightRenderInvalidation() {}

    /**
 * リソース全体の再読み込みや常時ポーリングを開始せず、必要なチャンク形状だけの再構築を要求する。
 */
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
