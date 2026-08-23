package dev.chise.chisetweaks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-based contract tests share repository access and token assertions here. */
final class SourceContractSupport {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    private SourceContractSupport() {}

    static String read(String relativePath) throws IOException {
        return Files.readString(path(relativePath));
    }

    static boolean exists(String relativePath) {
        return Files.exists(path(relativePath));
    }

    static void assertContainsAll(String source, String... tokens) {
        for (String token : tokens) {
            assertTrue(source.contains(token), () -> "missing source token: " + token);
        }
    }

    static void assertContainsNone(String source, String... tokens) {
        for (String token : tokens) {
            assertFalse(source.contains(token), () -> "unexpected source token: " + token);
        }
    }

    private static Path path(String relativePath) {
        return ROOT.resolve(relativePath).normalize();
    }
}
