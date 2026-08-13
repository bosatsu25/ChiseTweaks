package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Prevents third-party ore/resource-pack identity or asset references from entering production scope. */
final class OreHighlightsCleanRoomContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> FORBIDDEN_IDENTITIES = List.of(
            "amateras",
            "newglowingores",
            "glowingores");

    @Test
    void productionJavaAndResourcesRemainIndependentOfReplacedOrePacks() throws IOException {
        for (Path productionRoot : List.of(
                ROOT.resolve("src/main/java"),
                ROOT.resolve("src/main/resources"))) {
            try (var paths = Files.walk(productionRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String relative = ROOT.relativize(path).toString().toLowerCase(Locale.ROOT);
                    for (String identity : FORBIDDEN_IDENTITIES) {
                        assertFalse(relative.contains(identity),
                                () -> path + " contains third-party identity in its production path: " + identity);
                    }

                    // Only inspect text-like production files; binary assets must be independently generated/audited.
                    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!(fileName.endsWith(".java")
                            || fileName.endsWith(".json")
                            || fileName.endsWith(".json5")
                            || fileName.endsWith(".properties")
                            || fileName.endsWith(".mcmeta")
                            || fileName.endsWith(".txt"))) {
                        continue;
                    }

                    String content = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                    for (String identity : FORBIDDEN_IDENTITIES) {
                        assertFalse(content.contains(identity),
                                () -> path + " contains third-party production identity/reference: " + identity);
                    }
                }
            }
        }
    }
}
