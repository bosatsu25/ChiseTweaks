package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Prevents the reference fire resource pack's identity or copied fire assets from entering production. */
final class FireVisibilityCleanRoomContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> FORBIDDEN_IDENTITIES = List.of("lowonfire", "haikis");
    private static final Set<String> FORBIDDEN_REFERENCE_ASSETS = Set.of(
            "fire_0.png",
            "fire_1.png",
            "fire_2.png",
            "soul_fire_0.png",
            "soul_fire_1.png");

    @Test
    void productionRemainsIndependentOfReferencePackIdentityAndAssets() throws IOException {
        for (Path productionRoot : List.of(
                ROOT.resolve("src/main/java"),
                ROOT.resolve("src/main/resources"))) {
            try (var paths = Files.walk(productionRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String relative = ROOT.relativize(path).toString().toLowerCase(Locale.ROOT);
                    for (String identity : FORBIDDEN_IDENTITIES) {
                        assertFalse(relative.contains(identity),
                                () -> path + " contains reference-pack identity in its production path: " + identity);
                    }

                    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    assertFalse(FORBIDDEN_REFERENCE_ASSETS.contains(fileName),
                            () -> path + " copies a reference-pack fire texture instead of using Chise-owned rendering");

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
                                () -> path + " contains reference-pack identity/reference: " + identity);
                    }
                }
            }
        }
    }
}
