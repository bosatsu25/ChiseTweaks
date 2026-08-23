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
        assertTrue(session.contains("RuntimeDiagnostics.log(\"join\", client)"));
        assertTrue(session.contains("RuntimeDiagnostics.log(\"disconnect\", client)"));
        assertTrue(session.contains("SESSION_SEQUENCE.incrementAndGet()"));
    }

    @Test
    void quarantineAndResourceFailuresEmitDiagnosticSnapshots() throws IOException {
        String manager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String controller = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java"));
        assertTrue(manager.contains("RuntimeDiagnostics.log(\"component-quarantine-"));
        assertTrue(manager.contains("diagnosticQuarantinedComponentIds"));
        assertTrue(controller.contains("RuntimeDiagnostics.log(\"resource-reload-failure\""));
        assertTrue(controller.contains("TERMINAL_RECOVERY"));
        assertTrue(controller.contains("RELOADS.cancel("));
    }
}
