package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RepositoryRuntimeReviewContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void tickDispatchCannotDeadlockOnInitiallyInactiveComponents() throws IOException {
        String manager = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        assertTrue(manager.contains("component.tick(client);"));
        assertFalse(manager.contains("if (!active && !wasActive) return;"));
        assertFalse(manager.contains("private boolean wasActive;"));
    }

    @Test
    void oreRefreshIsAnIndependentListenerInsteadOfOverwritingExclusiveMode() throws IOException {
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        assertTrue(bindings.contains("MATERIAL_HIGHLIGHTS.addValueChangeListener"));
        assertFalse(bindings.contains("MATERIAL_HIGHLIGHTS.setValueChangeCallback"));
    }

    @Test
    void removedPoliciesAndLegacyOreAssetsCannotReturn() {
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/policy/BuilderEntityVisibilityPolicy.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/policy/ModVersionPolicy.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/models/block/visual/diamond_ore.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/models/block/visual/deepslate_diamond_ore.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/block/visual/diamond_ore_chise.png.mcmeta")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/block/visual/deepslate_diamond_ore_chise.png.mcmeta")));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
