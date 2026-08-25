package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level crash-containment contracts for optional visual rendering. */
final class OreHighlightFailSoftContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void resourcePackBaseIsEmittedBeforeAnyOptionalChiseWork() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        int baseEmit = model.indexOf("super.emitQuads(emitter, level, pos, state, random, cullTest);");
        int stateRead = model.indexOf("VisualRenderState.Snapshot renderState = VisualRenderState.current();", baseEmit);
        int optionalBranch = model.indexOf("if (!shouldRender(renderState)) return;", stateRead);
        int overlayEmit = model.indexOf("FullbrightOverlayEmission.emit(", optionalBranch);
        assertTrue(baseEmit >= 0);
        assertTrue(baseEmit < stateRead);
        assertTrue(stateRead < optionalBranch);
        assertTrue(optionalBranch < overlayEmit);
    }

    @Test
    void optionalOverlayLookupAndEmissionAreBoundedRecoverableAndFailSoft() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        String emission = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayEmission.java");

        assertTrue(model.contains("MAX_LOOKUP_ATTEMPTS = 3"));
        assertTrue(model.contains("catch (RuntimeException | LinkageError failure)"));
        assertTrue(model.contains("ORE_LOOKUP_FAILURE_LOGGED"));
        assertTrue(model.contains("GLASS_LOOKUP_FAILURE_LOGGED"));
        assertTrue(model.contains("KELP_LOOKUP_FAILURE_LOGGED"));
        assertTrue(model.contains("ORE_EMIT_FAILURE_LOGGED"));
        assertTrue(model.contains("GLASS_EMIT_FAILURE_LOGGED"));
        assertTrue(model.contains("KELP_EMIT_FAILURE_LOGGED"));
        assertTrue(model.contains("synchronized (this)"));
        assertFalse(model.contains("overlayResolved"));
        assertTrue(model.contains("FullbrightOverlayEmission.emit("));
        assertTrue(model.contains("emissionQuarantined"));
        assertTrue(model.contains("static FullbrightOverlayModel ore("));
        assertTrue(model.contains("static FullbrightOverlayModel glass("));
        assertTrue(model.contains("static FullbrightOverlayModel kelp("));

        assertTrue(emission.contains("boolean pushed = false"));
        assertTrue(emission.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(emission.contains("overlay.emitQuads(emitter, level, pos, state, random, cullTest)"));
        assertTrue(emission.contains("catch (RuntimeException | LinkageError emissionFailure)"));
        assertTrue(emission.contains("finally"));
        assertTrue(emission.contains("emitter.popTransform();"));

        assertTrue(model.contains("keeping the resource-pack base model without the Chise overlay"));
    }

    @Test
    void ordinaryChunkRendererInvalidationIsSharedCoalescedBoundedAndFailSoft() throws IOException {
        String invalidation = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ChunkRenderInvalidation.java");
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String builderFocus = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/BuilderFocusVisibility.java");
        assertTrue(invalidation.contains("MAX_FAILURE_RETRIES = 3"));
        assertTrue(invalidation.contains("REQUESTED.compareAndSet(false, true)"));
        assertTrue(invalidation.contains("client.execute(() -> refresh(client))"));
        assertTrue(invalidation.contains("catch (RuntimeException | LinkageError failure)"));
        assertTrue(invalidation.contains("if (retry < MAX_FAILURE_RETRIES)"));
        assertTrue(invalidation.contains("schedule();"));
        assertTrue(invalidation.contains("client.levelRenderer.allChanged()"));
        assertFalse(invalidation.contains("ClientTickEvents"));
        assertFalse(invalidation.contains("reloadResourcePacks"));
        assertTrue(builderFocus.contains("ChunkRenderInvalidation.request()"));
        assertFalse(builderFocus.contains("client.levelRenderer.allChanged()"));
        assertFalse(plugin.contains("ChunkRenderInvalidation.register"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightRenderInvalidation.java")));
    }

    @Test
    void rareModelMembershipReloadPreservesChangesArrivingDuringAsyncReload() throws IOException {
        String reload = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightModelReload.java");
        assertTrue(reload.contains("AtomicBoolean REQUESTED"));
        assertTrue(reload.contains("AtomicBoolean PENDING"));
        assertTrue(reload.contains("if (!REQUESTED.compareAndSet(false, true))"));
        assertTrue(reload.contains("PENDING.set(true)"));
        assertTrue(reload.contains("ChiseVisualModelPlugin.isModelPipelineReady()"));
        assertTrue(reload.contains("client.execute(() -> startReload(client))"));
        assertTrue(reload.contains("client.reloadResourcePacks().whenComplete"));
        assertTrue(reload.contains("catch (RuntimeException | LinkageError failure)"));
        assertTrue(reload.contains("if (PENDING.getAndSet(false)) request();"));
        assertTrue(reload.contains("resetAfterAbortedSchedule()"));
        assertFalse(reload.contains("ClientTickEvents"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
