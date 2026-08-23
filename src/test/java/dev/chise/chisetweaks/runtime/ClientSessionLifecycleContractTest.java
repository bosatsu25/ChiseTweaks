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
        int increment = source.indexOf("SESSION_SEQUENCE.incrementAndGet()");
        int joined = source.indexOf("phase = SessionPhase.JOINED");
        int reset = source.indexOf("FeatureManager.getInstance().resetSessionState(client)", joined);
        int recovery = source.indexOf("ChiseTexturePackController.onSessionStart(client)", reset);
        int log = source.indexOf("RuntimeDiagnostics.log(\"join\", client)", recovery);
        assertTrue(increment >= 0 && joined > increment && reset > joined && recovery > reset && log > recovery);
    }

    @Test
    void disconnectCapturesDiagnosticsBeforeCancellingReloadAndClearsRuntimeBeforeDisconnected() throws IOException {
        String source = Files.readString(SOURCE);
        int disconnecting = source.indexOf("phase = SessionPhase.DISCONNECTING");
        int log = source.indexOf("RuntimeDiagnostics.log(\"disconnect\", client)", disconnecting);
        int packEnd = source.indexOf("ChiseTexturePackController.onSessionEnd(client)", log);
        int reset = source.indexOf("FeatureManager.getInstance().resetSessionState(client)", packEnd);
        int disconnected = source.indexOf("phase = SessionPhase.DISCONNECTED", reset);
        assertTrue(disconnecting >= 0 && log > disconnecting && packEnd > log && reset > packEnd && disconnected > reset);
    }
}
