package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import net.minecraft.client.Minecraft;

/** Clears session-local visual caches on join/disconnect boundaries. */
public final class ClientSessionState {
    private ClientSessionState() {}

    public static void onJoin(Minecraft client) {
        OreHighlightResolver.invalidateCache();
        ClientCallbackCircuitBreaker.resetSessionState();
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.SESSION_FEATURE_RESET,
                () -> FeatureManager.getInstance().resetSessionState(client));
    }

    public static void onDisconnect(Minecraft client) {
        OreHighlightResolver.invalidateCache();
        ClientCallbackCircuitBreaker.resetSessionState();
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.SESSION_FEATURE_RESET,
                () -> FeatureManager.getInstance().resetSessionState(client));
    }
}
