package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class HardeningLifecycleContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void visibilityMigrationRunsBeforeConfigFilesCanBeCreated() throws IOException {
        String client = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java"));
        int registration = client.indexOf("SafeStartup.run(\"chise-texture-pack\"");
        int migration = client.indexOf("SafeStartup.run(\"visibility-pack-migration\"");
        int localConfig = client.indexOf("SafeStartup.run(\"local-config\"");
        int featureConfig = client.indexOf("SafeStartup.run(\"feature-config\"");
        assertTrue(registration >= 0);
        assertTrue(migration > registration);
        assertTrue(localConfig > migration);
        assertTrue(featureConfig > migration);
    }

    @Test
    void joinAndDisconnectDriveReloadRecoveryAndDiagnostics() throws IOException {
        String session = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java"));
        assertTrue(session.contains("ChiseTexturePackController.onSessionStart(client)"));
        assertTrue(session.contains("ChiseTexturePackController.onSessionEnd(client)"));
        assertTrue(session.contains("RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_JOIN, client)"));
        assertTrue(session.contains("RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_DISCONNECT, client)"));
        assertTrue(session.contains("SESSION_SEQUENCE.incrementAndGet()"));
    }

    @Test
    void quarantineAndResourceFailuresEmitTypedDiagnosticSnapshots() throws IOException {
        String manager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String controller = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java"));
        String renderGuard = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallRenderGuard.java"));
        assertTrue(manager.contains("RuntimeDiagnosticEvent.COMPONENT_INIT_QUARANTINE"));
        assertTrue(manager.contains("RuntimeDiagnosticEvent.COMPONENT_QUARANTINE"));
        assertTrue(manager.contains("diagnosticQuarantinedComponentIds"));
        assertTrue(controller.contains("RuntimeDiagnosticEvent.RESOURCE_RELOAD_FAILURE"));
        assertTrue(controller.contains("RuntimeDiagnosticEvent.RESOURCE_RELOAD_TERMINAL_FAILURE"));
        assertTrue(controller.contains("TERMINAL_RECOVERY"));
        assertTrue(controller.contains("RELOADS.cancel("));
        assertTrue(renderGuard.contains("RuntimeDiagnosticEvent.COMPONENT_QUARANTINE"));
        assertTrue(renderGuard.contains("RuntimeDiagnosticDetail.of(\"stage\", \"render\")"));
        assertTrue(renderGuard.contains("snapshot.clear()"));
        assertTrue(renderGuard.contains("renderer.resetAfterFailure()"));
    }
}
