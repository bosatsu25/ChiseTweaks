package dev.chise.chisetweaks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RepositoryScopeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void removedProductionFamiliesHaveNoJavaOrMixinResidue() throws IOException {
        List<String> forbidden = List.of(
                "PUMPKIN_SCAFFOLD",
                "PLACEMENT_GUIDE",
                "PumpkinScaffold",
                "PlacementGuide",
                "SODIUM_LAVA_HIGHLIGHT",
                "LavaHighlightRendererMixin",
                "LavaFluidRenderHandler",
                "ExternalHookCircuitBreaker");

        try (var paths = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                for (String token : forbidden) {
                    assertFalse(source.contains(token), () -> path + " still contains removed token " + token);
                }
            }
        }

        assertFalse(exists("src/main/resources/chisetweaks.sodium.mixins.json"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/mixin/sodium/LavaHighlightRendererMixin.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightConfig.java"));
    }

    @Test
    void lavaSourceHighlightUsesBoundedLoadedChunkDiscoveryAndSharedRetainedRendering() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");

        assertContainsAll(feature,
                "fluidState.isSource()",
                "getChunkNow",
                "LevelChunk[] loadedChunkBuffer",
                "MAX_OVERLAY_RESULTS",
                "ThroughWallPositionSnapshot",
                "NearestPositionBuffer",
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "LavaVisionPalettePolicy.shouldHighlight",
                "Lava Source Highlight initialized");
        assertContainsAll(renderer,
                "withDepthStencilState(Optional.empty())",
                "LavaVisionPalettePolicy.colorForDistance",
                "sources.renderRevision() != uploadedRevision",
                "RetainedThroughWallBuffer",
                "ThroughWallWireBoxGeometry.drawWireBox");
        assertContainsAll(retained,
                "anchorX - camera.x",
                "vertexBuffer.rotate()");
        assertContainsNone(feature, "DefaultFluidRenderer", ".getChunk(");
        assertContainsNone(renderer, "DefaultFluidRenderer", "getFluidState(", "getBlockState(");
    }

    @Test
    void lavaSourceHighlightGuardsSessionConfigChunkEdgesAndRuntimeFailures() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String manager = read("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");

        assertContainsAll(feature,
                "lastLevel != client.level",
                "client.level != lastLevel",
                "fingerprint != lastScanFingerprint",
                "hasKnownSourceBoundary",
                "getChunkNow(neighborChunkX, neighborChunkZ)",
                "neighborChunk == null",
                "runtimeQuarantined",
                "!isSessionQuarantined()",
                "public void onQuarantined(Minecraft client)",
                "MAX_STABLE_BACKOFF_SHIFT = 2");
        assertContainsNone(feature,
                "local.lavaHighlightEnabled = false",
                ".save()");
        assertContainsAll(manager,
                "notifyInitializationQuarantine(component.getId(), ticking)",
                "removeFromSchedules(component)");
        assertContainsAll(renderer,
                "resetAfterFailure()",
                "uploadedRevision = Long.MIN_VALUE");
        assertContainsAll(retained,
                "drawVertexBuffer = null",
                "vertexBuffer.close()",
                "allocator = new ByteBufferBuilder");
    }

    @Test
    void fabricMetadataRemainsStrictlyClientOnlyAndStandalone() throws IOException {
        JsonObject root = JsonParser.parseString(read("src/main/resources/fabric.mod.json")).getAsJsonObject();
        assertEquals("chisetweaks", root.get("id").getAsString());
        assertEquals("client", root.get("environment").getAsString());
        assertEquals(Set.of("client", "modmenu"), root.getAsJsonObject("entrypoints").keySet());
        assertEquals(1, root.getAsJsonArray("mixins").size());
        assertEquals("chisetweaks.features.mixins.json", root.getAsJsonArray("mixins").get(0).getAsString());

        JsonObject custom = root.getAsJsonObject("custom").getAsJsonObject("chisetweaks");
        assertEquals("client-only", custom.get("side").getAsString());
        assertFalse(custom.get("serverInstallationRequired").getAsBoolean());
        assertFalse(custom.get("customPlayProtocol").getAsBoolean());
        assertFalse(custom.get("remoteModDetection").getAsBoolean());
        assertFalse(custom.get("backgroundThreads").getAsBoolean());
        assertFalse(custom.get("automaticModDownload").getAsBoolean());
        assertFalse(custom.get("automaticJarReplacement").getAsBoolean());
        assertFalse(custom.get("modMenuRequired").getAsBoolean());
    }

    @Test
    void requiredVerificationFilesAndIndependentGradleGatesArePresent() throws IOException {
        assertTrue(exists(".github/workflows/ci.yml"));
        assertTrue(exists(".github/workflows/verify-build.yml"));
        assertTrue(exists(".github/workflows/release.yml"));
        assertFalse(exists(".github/workflows/verified-release.yml"));
        assertTrue(exists("scripts/repository_audit.py"));
        assertTrue(exists("scripts/quality_summary.py"));
        assertTrue(exists("scripts/artifact_audit.py"));
        assertFalse(exists("scripts/local_ci.py"));

        String build = read("build.gradle");
        assertContainsAll(build,
                "id 'jacoco'",
                "id 'info.solidsoft.pitest'",
                "tasks.register('qualityGate')",
                "tasks.register('ciGate')",
                "mutationThreshold",
                "testStrengthThreshold");
    }
}
