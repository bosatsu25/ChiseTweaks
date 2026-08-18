package dev.chise.chisetweaks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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

        assertFalse(Files.exists(ROOT.resolve("src/main/resources/chisetweaks.sodium.mixins.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/mixin/sodium/LavaHighlightRendererMixin.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightConfig.java")));
    }

    @Test
    void lavaSourceHighlightUsesTheBoundedStandaloneRendererPath() throws IOException {
        String feature = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java"));

        assertTrue(feature.contains("fluidState.isSource()"));
        assertTrue(feature.contains("hasChunk"));
        assertTrue(feature.contains("MAX_OVERLAY_RESULTS"));
        assertTrue(feature.contains("LavaVisionPalettePolicy.shouldHighlight"));
        assertTrue(feature.contains("Lava Source Highlight initialized"));
        assertTrue(renderer.contains("withDepthStencilState(Optional.empty())"));
        assertTrue(renderer.contains("LavaVisionPalettePolicy.colorForDistance"));
        assertFalse(feature.contains("DefaultFluidRenderer"));
        assertFalse(renderer.contains("DefaultFluidRenderer"));
    }

    @Test
    void lavaSourceHighlightGuardsSessionConfigChunkEdgesAndRuntimeFailures() throws IOException {
        String feature = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"));
        String manager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java"));

        assertTrue(feature.contains("lastLevel != client.level"));
        assertTrue(feature.contains("client.level != lastLevel"));
        assertTrue(feature.contains("fingerprint != lastScanFingerprint"));
        assertTrue(feature.contains("hasKnownSourceBoundary"));
        assertTrue(feature.contains("hasChunk(neighborChunkX, neighborChunkZ)"));
        assertTrue(feature.contains("local.lavaHighlightEnabled = false"));
        assertTrue(feature.contains("runtimeQuarantined"));
        assertTrue(feature.contains("!isSessionQuarantined()"));
        assertTrue(feature.contains("public void onQuarantined(Minecraft client)"));
        assertTrue(manager.contains("notifyInitializationQuarantine(feature.getId(), ticking)"));
        assertTrue(renderer.contains("if (!geometryComplete) buffer = null"));
        assertTrue(renderer.contains("builtBuffer = buffer.buildOrThrow()"));
        assertTrue(renderer.contains("Never reuse a builder after either a successful build"));
    }

    @Test
    void fabricMetadataRemainsStrictlyClientOnlyAndStandalone() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(
                ROOT.resolve("src/main/resources/fabric.mod.json"))).getAsJsonObject();
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
    void requiredVerificationFilesArePresent() throws IOException {
        assertTrue(Files.exists(ROOT.resolve(".github/workflows/ci.yml")));
        assertTrue(Files.exists(ROOT.resolve(".github/workflows/verify-build.yml")));
        assertTrue(Files.exists(ROOT.resolve(".github/workflows/release.yml")));
        assertFalse(Files.exists(ROOT.resolve(".github/workflows/verified-release.yml")));
        assertTrue(Files.exists(ROOT.resolve("scripts/repository_audit.py")));
        assertTrue(Files.exists(ROOT.resolve("scripts/quality_summary.py")));
        assertTrue(Files.exists(ROOT.resolve("scripts/artifact_audit.py")));
        assertFalse(Files.exists(ROOT.resolve("scripts/local_ci.py")));

        String build = Files.readString(ROOT.resolve("build.gradle"));
        assertTrue(build.contains("id 'jacoco'"));
        assertTrue(build.contains("id 'info.solidsoft.pitest'"));
        assertTrue(build.contains("tasks.register('qualityGate')"));
        assertTrue(build.contains("mutationThreshold"));
        assertTrue(build.contains("testStrengthThreshold"));
    }
}
