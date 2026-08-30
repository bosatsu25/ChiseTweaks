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
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java"));
    }

    @Test
    void occludedHighlightsUseBoundedLoadedChunkDiscoveryAndOwnedRetainedRendering() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/OccludedHighlightsFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertContainsAll(feature,
                "int dueMask = 0",
                "LoadedChunkWindow loadedChunks",
                "loadedChunks.load(",
                "loadedChunks.atBlock(",
                "MAX_CANDIDATES",
                "ThroughWallPositionSnapshot",
                "NearestBuffer",
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "ThroughWallMarkerRenderer.Style.HIDDEN_BLOCK",
                "LavaVisionPalettePolicy.shouldHighlight",
                "populateHiddenTargetMasks();",
                "int targetMask = hiddenTargetMask(block)",
                "(local.visualTargetMask & targetMask) != 0");
        assertContainsAll(renderer,
                "withDepthStencilState(Optional.empty())",
                "private static final class RetainedBuffer",
                "anchorX - camera.x",
                "vertexBuffer.rotate()",
                "ThroughWallWireBoxGeometry.drawWireBox");
        assertContainsNone(feature,
                "DefaultFluidRenderer",
                ".getChunk(",
                "BlockInspectionPolicy.matches",
                "BlockInspectionCategory.HIDDEN_SURFACE");
        assertContainsAll(renderer,
                "private String hiddenBlockIdAt(",
                "getChunkSource().getChunkNow(");
        assertContainsNone(renderer, "DefaultFluidRenderer", "getFluidState(");
    }

    @Test
    void occludedHighlightsGuardLifecycleConfigAndRuntimeFailures() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/OccludedHighlightsFeature.java");
        String manager = read("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String runtime = read("src/main/java/dev/chise/chisetweaks/runtime/RuntimeComponent.java");

        assertContainsAll(feature,
                "lastLevel != client.level",
                "client.level != lastLevel",
                "fingerprint != lastScanFingerprint[index]",
                "hasKnownSourceBoundary",
                "runtimeQuarantined",
                "public void onQuarantined(Minecraft client)",
                "public void resetSession(Minecraft client)",
                "MAX_STABLE_BACKOFF_SHIFT = 2");
        assertContainsNone(feature,
                "local.lavaHighlightEnabled = false",
                "local.hiddenSurfaceTraceEnabled = false",
                ".save()");
        assertContainsAll(manager,
                "slot.quarantineDuringInitialization(Minecraft.getInstance(), failure)",
                "removeFromSchedules(slot)",
                "component.onQuarantined(client)",
                "for (ComponentSlot slot : sessionSchedule) slot.resetSession(client)");
        assertContainsAll(runtime, "default void onQuarantined(Minecraft client)");
    }

    @Test
    void fabricMetadataRemainsStrictlyClientOnlyAndStandalone() throws IOException {
        JsonObject root = JsonParser.parseString(read("src/main/resources/fabric.mod.json")).getAsJsonObject();
        assertEquals("chisetweaks", root.get("id").getAsString());
        assertEquals("client", root.get("environment").getAsString());
        assertEquals(Set.of("client", "modmenu"), root.getAsJsonObject("entrypoints").keySet());
        assertEquals(List.of(
                        "chisetweaks.features.mixins.json",
                        "chisetweaks.integrations.mixins.json",
                        "chisetweaks.inspector.mixins.json"),
                root.getAsJsonArray("mixins").asList().stream()
                        .map(element -> element.getAsString())
                        .toList());

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
    void requiredVerificationFilesAndSingleWorkflowGateArePresent() throws IOException {
        assertTrue(exists(".github/workflows/ci.yml"));
        assertFalse(exists(".github/workflows/release.yml"));
        assertFalse(exists(".github/workflows/verify-build.yml"));
        assertFalse(exists(".github/workflows/verified-release.yml"));
        assertTrue(exists("scripts/repository_audit.py"));
        assertTrue(exists("scripts/artifact_audit.py"));

        String build = read("build.gradle");
        String ci = read(".github/workflows/ci.yml");
        assertContainsAll(build,
                "id 'jacoco'",
                "id 'info.solidsoft.pitest'",
                "tasks.register('qualityGate')",
                "tasks.register('ciGate')",
                "mutationThreshold",
                "testStrengthThreshold");
        assertContainsAll(ci,
                "name: verify / Java 25 quality gate",
                "name: release / Publish verified runtime JAR",
                "./gradlew --stacktrace ciGate",
                "./gradlew --stacktrace runClientGameTest");
    }
}
