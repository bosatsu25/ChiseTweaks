package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RendererCompatibilityArchitectureContractTest {
    @Test
    void nvidiumRemainsOptionalAndApiDriftStaysFailSoft() throws Exception {
        String fabric = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String bridge = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/integration/compat/NvidiumCompatibility.java"));
        String component = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/integration/compat/WorldBorderFixComponent.java"));

        assertFalse(fabric.contains("\"nvidium\":"));
        assertFalse(fabric.contains("\"sodium\":"));
        assertTrue(bridge.contains("isModLoaded(\"nvidium\")"));
        assertTrue(bridge.contains("Class.forName(\"me.cortex.nvidium.Nvidium\""));
        assertTrue(bridge.contains("ReflectiveOperationException | LinkageError"));
        assertTrue(component.contains("if (!NvidiumCompatibility.isAvailable()) return;"));
    }
}
