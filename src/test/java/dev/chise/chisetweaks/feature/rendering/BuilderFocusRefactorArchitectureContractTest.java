package dev.chise.chisetweaks.feature.rendering;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuilderFocusRefactorArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void builderFocusCompilesOnlyChangedFingerprintsAndUsesDirectRegistryResolution() throws Exception {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/BuilderFocusVisibility.java"));

        assertTrue(source.contains("EntityConfigFingerprint"));
        assertTrue(source.contains("if (next.equals(entityFingerprint)) return;"));
        assertTrue(source.contains("BuiltInRegistries.BLOCK.containsKey(id)"));
        assertTrue(source.contains("BuiltInRegistries.BLOCK.getValue(id)"));
        assertTrue(source.contains("setEntityRulesSilently("));
        assertTrue(source.contains("ENTITY_RULE_MODE.setValueSilently("));
        assertTrue(source.contains("ENTITY_BLACKLIST.setStringsSilently("));
        assertFalse(source.contains("for (Block block : BuiltInRegistries.BLOCK)"));
    }

    @Test
    void editorValidationUsesRegistryKeyLookupsInsteadOfFullRegistryScans() throws Exception {
        String backend = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseListEditorBackend.java"));
        String scene = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/SceneFilterBackend.java"));
        String masa = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/MasaListBackend.java"));

        assertTrue(backend.contains("BuiltInRegistries.BLOCK.containsKey(id)"));
        assertTrue(backend.contains("BuiltInRegistries.ENTITY_TYPE.containsKey(id)"));
        assertTrue(backend.contains("BuiltInRegistries.ITEM.containsKey(id)"));
        assertFalse(scene.contains("for (EntityType<?>"));
        assertFalse(masa.contains("for (Item item"));
    }

    @Test
    void worksiteRenderPathComparesPrecomputedBlockIdentity() throws Exception {
        String target = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteRenderTarget.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java"));

        assertTrue(target.contains("Block expectedBlock"));
        assertTrue(target.contains("BuiltInRegistries.BLOCK.containsKey(id)"));
        assertTrue(renderer.contains("target.expectedBlock() != liveBlock"));
        assertFalse(renderer.contains("BuiltInRegistries.BLOCK.getKey("));
    }
}
