package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientSessionLifecycleContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java");

    @Test
    void joinPublishesSessionBeforeResetAndDiagnostics() throws IOException {
        String source = Files.readString(SOURCE);
        int method = source.indexOf("public static void onJoin(Minecraft client)");
        int increment = source.indexOf("SESSION_SEQUENCE.incrementAndGet()", method);
        int joined = source.indexOf("phase = SessionPhase.JOINED", increment);
        int reset = source.indexOf("reset(client);", joined);
        int log = source.indexOf("RuntimeDiagnosticEvent.CLIENT_JOIN", reset);
        assertTrue(method >= 0 && increment > method && joined > increment
                && reset > joined && log > reset);
        assertFalse(source.contains("ChiseTexturePackController"));
    }

    @Test
    void disconnectCapturesDiagnosticsBeforeClearingRuntimeAndPublishingDisconnected() throws IOException {
        String source = Files.readString(SOURCE);
        int method = source.indexOf("public static void onDisconnect(Minecraft client)");
        int disconnecting = source.indexOf("phase = SessionPhase.DISCONNECTING", method);
        int log = source.indexOf("RuntimeDiagnosticEvent.CLIENT_DISCONNECT", disconnecting);
        int reset = source.indexOf("reset(client);", log);
        int disconnected = source.indexOf("phase = SessionPhase.DISCONNECTED", reset);
        assertTrue(method >= 0 && disconnecting > method && log > disconnecting
                && reset > log && disconnected > reset);
        assertFalse(source.contains("onSessionEnd"));
        assertFalse(source.contains("RESOURCE_RELOAD"));
    }

    @Test
    void resetHelperClearsCachesAndCircuitBreakersBeforeFeatureSessionState() throws IOException {
        String source = Files.readString(SOURCE);
        int method = source.indexOf("private static void reset(Minecraft client)");
        int oreCache = source.indexOf("OreHighlightResolver.invalidateCache()", method);
        int circuitBreaker = source.indexOf("ClientCallbackCircuitBreaker.resetSessionState()", oreCache);
        int featureReset = source.indexOf("FeatureManager.getInstance().resetSessionState(client)", circuitBreaker);
        assertTrue(method >= 0 && oreCache > method && circuitBreaker > oreCache && featureReset > circuitBreaker);
    }
}
