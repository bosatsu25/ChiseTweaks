package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientSessionLifecycleContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java");

    @Test
    void joinPublishesSessionBeforeResetAndRecovery() throws IOException {
        String source = Files.readString(SOURCE);
        int method = source.indexOf("public static void onJoin(Minecraft client)");
        int increment = source.indexOf("SESSION_SEQUENCE.incrementAndGet()", method);
        int joined = source.indexOf("phase = SessionPhase.JOINED", increment);
        int reset = source.indexOf("reset(client);", joined);
        int recovery = source.indexOf("ChiseTexturePackController.onSessionStart(client)", reset);
        int log = source.indexOf("RuntimeDiagnostics.log(\"join\", client)", recovery);
        assertTrue(method >= 0 && increment > method && joined > increment
                && reset > joined && recovery > reset && log > recovery);
    }

    @Test
    void disconnectCapturesDiagnosticsBeforeCancellingReloadAndClearsRuntimeBeforeDisconnected() throws IOException {
        String source = Files.readString(SOURCE);
        int method = source.indexOf("public static void onDisconnect(Minecraft client)");
        int disconnecting = source.indexOf("phase = SessionPhase.DISCONNECTING", method);
        int log = source.indexOf("RuntimeDiagnostics.log(\"disconnect\", client)", disconnecting);
        int packEnd = source.indexOf("ChiseTexturePackController.onSessionEnd(client)", log);
        int reset = source.indexOf("reset(client);", packEnd);
        int disconnected = source.indexOf("phase = SessionPhase.DISCONNECTED", reset);
        assertTrue(method >= 0 && disconnecting > method && log > disconnecting
                && packEnd > log && reset > packEnd && disconnected > reset);
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
