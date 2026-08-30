package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Enforces the production dependency direction at source level without adding
 * an architecture library to the runtime or test classpath.
 */
final class ArchitectureBoundaryContractTest {
    private static final Path MAIN = Path.of("src/main/java/dev/chise/chisetweaks");

    private static final Map<String, List<String>> FORBIDDEN_DEPENDENCIES = Map.of(
            "core", List.of("config", "feature", "gui", "integration", "mixin", "runtime", "compat"),
            "config", List.of("feature", "gui", "mixin", "runtime", "compat"),
            "feature", List.of("gui", "integration", "mixin"),
            "integration", List.of("gui", "feature", "mixin"));

    @Test
    void productionLayersDoNotAcquireForbiddenUpwardDependencies() throws IOException {
        for (var entry : FORBIDDEN_DEPENDENCIES.entrySet()) {
            Path layerRoot = MAIN.resolve(entry.getKey());
            if (!Files.isDirectory(layerRoot)) continue;

            try (var paths = Files.walk(layerRoot)) {
                for (Path sourcePath : paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList()) {
                    String source = Files.readString(sourcePath);
                    for (String forbidden : entry.getValue()) {
                        String importPrefix = "import dev.chise.chisetweaks." + forbidden + ".";
                        assertFalse(source.contains(importPrefix),
                                () -> entry.getKey() + " must not depend on " + forbidden + ": " + sourcePath);
                    }
                }
            }
        }
    }

    @Test
    void coreRemainsIndependentFromApplicationBootstrap() throws IOException {
        Path core = MAIN.resolve("core");
        try (var paths = Files.walk(core)) {
            for (Path sourcePath : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(sourcePath);
                assertFalse(source.contains("import dev.chise.chisetweaks.ChiseTweaksClient;"),
                        () -> "core must not depend on application bootstrap: " + sourcePath);
            }
        }
    }
}
