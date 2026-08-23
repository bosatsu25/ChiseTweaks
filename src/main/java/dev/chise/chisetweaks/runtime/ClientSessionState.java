package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicLong;

public final class ClientSessionState {
    public enum SessionPhase {
        STARTING,
        JOINED,
        DISCONNECTING,
        DISCONNECTED
    }

    private static final AtomicLong SESSION_SEQUENCE = new AtomicLong();
    private static volatile long currentSessionId;
    private static volatile SessionPhase phase = SessionPhase.STARTING;

    private ClientSessionState() {}

    public static void onJoin(Minecraft client) {
        currentSessionId = SESSION_SEQUENCE.incrementAndGet();
        phase = SessionPhase.JOINED;
        reset(client);
        ChiseTexturePackController.onSessionStart(client);
        RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_JOIN, client);
    }

    public static void onDisconnect(Minecraft client) {
        phase = SessionPhase.DISCONNECTING;
        RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_DISCONNECT, client);
        ChiseTexturePackController.onSessionEnd(client);
        reset(client);
        phase = SessionPhase.DISCONNECTED;
    }

    public static long currentSessionId() {
        return currentSessionId;
    }

    public static SessionPhase phase() {
        return phase;
    }

    private static void reset(Minecraft client) {
        OreHighlightResolver.invalidateCache();
        ClientCallbackCircuitBreaker.resetSessionState();
        FeatureManager.getInstance().resetSessionState(client);
    }
}
