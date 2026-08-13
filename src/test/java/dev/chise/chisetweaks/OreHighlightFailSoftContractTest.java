package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level crash-containment contracts for optional Ore Highlight rendering. */
final class OreHighlightFailSoftContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void resourcePackBaseIsEmittedBeforeAnyOptionalChiseWork() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        int baseEmit = model.indexOf("super.emitQuads(emitter, level, pos, state, random, cullTest);");
        int overlayLookup = model.indexOf("BlockStateModel overlay = overlayModel(animated);");
        int overlayEmit = model.indexOf("overlay.emitQuads(emitter, level, pos, state, random, cullTest);");
        assertTrue(baseEmit >= 0);
        assertTrue(baseEmit < overlayLookup);
        assertTrue(overlayLookup < overlayEmit);
    }

    @Test
    void optionalOverlayLookupAndEmissionCannotThrowIntoChunkCompilation() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        assertTrue(model.contains("catch (RuntimeException failure)"));
        assertTrue(model.contains("LOOKUP_FAILURE_LOGGED"));
        assertTrue(model.contains("EMIT_FAILURE_LOGGED"));
        assertTrue(model.contains("keeping the resource-pack base model without the Chise overlay"));
        assertTrue(model.contains("emitter.popTransform();"));
    }

    @Test
    void rendererInvalidationIsBoundedFailSoftAndNeverReloadsResources() throws IOException {
        String invalidation = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightRenderInvalidation.java");
        assertTrue(invalidation.contains("MAX_FAILURE_RETRIES = 3"));
        assertTrue(invalidation.contains("catch (RuntimeException failure)"));
        assertTrue(invalidation.contains("if (retry < MAX_FAILURE_RETRIES)"));
        assertTrue(invalidation.contains("REQUESTED.set(true)"));
        assertTrue(invalidation.contains("client.levelRenderer.allChanged()"));
        assertFalse(invalidation.contains("reloadResourcePacks"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
