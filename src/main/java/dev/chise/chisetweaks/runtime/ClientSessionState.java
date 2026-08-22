package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import net.minecraft.client.Minecraft;

public final class ClientSessionState {
    private ClientSessionState() {}

    public static void onJoin(Minecraft client) {
        reset(client);
    }

    public static void onDisconnect(Minecraft client) {
        reset(client);
    }

    private static void reset(Minecraft client) {
        OreHighlightResolver.invalidateCache();
        ClientCallbackCircuitBreaker.resetSessionState();
        FeatureManager.getInstance().resetSessionState(client);
    }
}
