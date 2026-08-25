package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HardeningLifecycleContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void configLoadsBeforeRuntimeBindingsWithoutRetiredVisibilityMigration() throws IOException {
        String client = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java"));
        int localConfig = client.indexOf("SafeStartup.run(\"local-config\"");
        int featureConfig = client.indexOf("SafeStartup.run(\"feature-config\"");
        int bindings = client.indexOf("SafeStartup.run(\"feature-bindings\"");
        int modelPlugin = client.indexOf("SafeStartup.run(\"visual-model-plugin\"");
        int manager = client.indexOf("SafeStartup.run(\"feature-manager\"");
        assertTrue(localConfig >= 0);
        assertTrue(featureConfig > localConfig);
        assertTrue(bindings > featureConfig);
        assertTrue(modelPlugin > bindings);
        assertTrue(manager > modelPlugin);
        assertFalse(client.contains("chise-texture-pack"));
        assertFalse(client.contains("visibility-pack-migration"));
        assertFalse(client.contains("ChiseTexturePack"));
    }

    @Test
    void joinAndDisconnectDriveDiagnosticsAndRuntimeResetWithoutPackRecovery() throws IOException {
        String session = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java"));
        assertTrue(session.contains("RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_JOIN, client)"));
        assertTrue(session.contains("RuntimeDiagnostics.log(RuntimeDiagnosticEvent.CLIENT_DISCONNECT, client)"));
        assertTrue(session.contains("SESSION_SEQUENCE.incrementAndGet()"));
        assertTrue(session.contains("FeatureManager.getInstance().resetSessionState(client)"));
        assertFalse(session.contains("ChiseTexturePackController"));
        assertFalse(session.contains("RESOURCE_RELOAD"));
    }

    @Test
    void quarantineFailuresRemainTypedWhileRetiredPackFailurePathStaysAbsent() throws IOException {
        String manager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String diagnostics = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/RuntimeDiagnostics.java"));
        String renderGuard = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallRenderGuard.java"));
        assertTrue(manager.contains("RuntimeDiagnosticEvent.COMPONENT_INIT_QUARANTINE"));
        assertTrue(manager.contains("RuntimeDiagnosticEvent.COMPONENT_QUARANTINE"));
        assertTrue(manager.contains("diagnosticQuarantinedComponentIds"));
        assertTrue(renderGuard.contains("RuntimeDiagnosticEvent.COMPONENT_QUARANTINE"));
        assertTrue(renderGuard.contains("RuntimeDiagnosticDetail.of(\"stage\", \"render\")"));
        assertTrue(renderGuard.contains("snapshot.clear()"));
        assertTrue(renderGuard.contains("renderer.resetAfterFailure()"));
        assertFalse(diagnostics.contains("ChiseTexturePackController"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java")));
    }
}
