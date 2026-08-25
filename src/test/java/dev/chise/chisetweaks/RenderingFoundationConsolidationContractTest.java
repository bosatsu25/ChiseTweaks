package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents the consolidated rendering/config foundations from drifting back into per-feature duplication. */
final class RenderingFoundationConsolidationContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void modelHighlightsShareOneFailSoftWrapper() throws IOException {
        Path modelRoot = ROOT.resolve("src/main/java/dev/chise/chisetweaks/feature/rendering/model");
        assertFalse(Files.exists(modelRoot.resolve("FullbrightOreHighlightModel.java")));
        assertFalse(Files.exists(modelRoot.resolve("FullbrightGlassHighlightModel.java")));
        assertFalse(Files.exists(modelRoot.resolve("FullbrightKelpHighlightModel.java")));

        String shared = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        assertTrue(shared.contains("MAX_LOOKUP_ATTEMPTS = 3"));
        assertTrue(shared.contains("static FullbrightOverlayModel ore("));
        assertTrue(shared.contains("static FullbrightOverlayModel glass("));
        assertTrue(shared.contains("static FullbrightOverlayModel kelp("));
        assertTrue(shared.contains("renderState.shouldRenderOre(target)"));
        assertTrue(shared.contains("renderState.glassEnabled()"));
        assertTrue(shared.contains("renderState.kelpEnabled()"));
        assertTrue(shared.contains("FullbrightOverlayEmission.emit("));
        assertTrue(plugin.contains("FullbrightOverlayModel.ore("));
        assertTrue(plugin.contains("FullbrightOverlayModel.glass("));
        assertTrue(plugin.contains("FullbrightOverlayModel.kelp("));
    }

    @Test
    void retainedLavaAnalyzerUsesSharedRenderQuarantineLifecycle() throws IOException {
        String guard = source("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallRenderGuard.java");
        String lava = source("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(guard.contains("catch (RuntimeException | LinkageError failure)"));
        assertTrue(guard.contains("RuntimeDiagnosticEvent.COMPONENT_QUARANTINE"));
        assertTrue(guard.contains("renderer.resetAfterFailure()"));
        assertTrue(lava.contains("new ThroughWallRenderGuard("));
        assertFalse(lava.contains("boolean renderQuarantined"));
        assertTrue(lava.contains("scanLoadedSources("));
        assertFalse(lava.contains("scanChunkIntoBuffer("));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java")));
    }

    @Test
    void worksiteMaterializesPropertiesOnceWithoutTransientCarrier() throws IOException {
        Path carrier = ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteMaterializedInspection.java");
        String scanner = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");
        assertFalse(Files.exists(carrier));
        assertTrue(scanner.contains("var properties = blockInspector.properties(candidate.state());"));
        assertTrue(scanner.contains("BlockInspectionPolicy.inspect(\n                candidate.blockId(), properties, candidate.category())"));
        assertTrue(scanner.contains("OrientationOverlayPolicy.inspect(properties)"));
    }

    @Test
    void uiSettingsNoLongerRequireNoOpInitializationOrDuplicateVisualTargetCopy() throws IOException {
        String client = source("src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java");
        String local = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targets = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertFalse(client.contains("LocalFeatureSettings::init"));
        assertFalse(client.contains("VisualTargetSettings::init"));
        assertFalse(local.contains("public static void init()"));
        assertFalse(targets.contains("public static void init()"));
        assertFalse(targets.contains("Coal Ore"));
        assertFalse(targets.contains("石炭鉱石"));
        assertTrue(targets.contains("visualTargetMaterialCoalOre"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
