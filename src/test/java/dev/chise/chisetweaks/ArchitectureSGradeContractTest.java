package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** S1: executable package-boundary contract for production architecture. */
final class ArchitectureSGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path MAIN = ROOT.resolve("src/main/java/dev/chise/chisetweaks");

    @Test
    void coreRemainsIndependentFromHigherLevelPackages() throws IOException {
        assertNoImports("core", Set.of("config", "runtime", "feature", "integration", "gui", "mixin"));
    }

    @Test
    void configCannotReachPresentationRuntimeOrMixinLayers() throws IOException {
        assertNoImports("config", Set.of("runtime", "feature", "gui", "mixin"));
    }

    @Test
    void apiRemainsIndependentFromImplementationLayers() throws IOException {
        assertNoImports("api", Set.of("config", "runtime", "feature", "integration", "gui", "mixin"));
    }

    private static void assertNoImports(String ownerPackage, Set<String> forbiddenPackages) throws IOException {
        Path ownerRoot = MAIN.resolve(ownerPackage);
        List<String> violations = new ArrayList<>();
        if (!Files.isDirectory(ownerRoot)) return;

        try (Stream<Path> files = Files.walk(ownerRoot)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                for (String forbidden : forbiddenPackages) {
                    String marker = "import dev.chise.chisetweaks." + forbidden + ".";
                    if (source.contains(marker)) {
                        violations.add(ROOT.relativize(path) + " -> " + forbidden);
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Architecture dependency violations:\n" + String.join("\n", violations));
    }
}
