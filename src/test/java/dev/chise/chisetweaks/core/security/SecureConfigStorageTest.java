package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SecureConfigStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void atomicWriteAndStrictReadRoundTrip() throws Exception {
        Path root = configRoot();

        SecureConfigStorage.writeUtf8Atomic(root, "chisetweaks.json", "{\"enabled\":true}", 1024);

        assertEquals("{\"enabled\":true}",
                SecureConfigStorage.readUtf8(root, "chisetweaks.json", 1024).orElseThrow());
        assertTrue(SecureConfigStorage.isSafeTarget(root, "chisetweaks.json"));
    }

    @Test
    void nullContentBecomesEmptyStrictUtf8File() throws Exception {
        Path root = configRoot();

        SecureConfigStorage.writeUtf8Atomic(root, "empty.json", null, 1024);

        assertEquals("", SecureConfigStorage.readUtf8(root, "empty.json", 1024).orElseThrow());
        assertEquals(0, Files.size(root.resolve("empty.json")));
    }

    @Test
    void missingFileAndMissingRootReturnEmptyWithoutCreatingAnything() throws Exception {
        Path missingRoot = tempDir.resolve("missing-config");

        assertTrue(SecureConfigStorage.readUtf8(missingRoot, "settings.json", 1024).isEmpty());
        assertFalse(Files.exists(missingRoot));

        Path root = configRoot();
        assertTrue(SecureConfigStorage.readUtf8(root, "missing.json", 1024).isEmpty());
        assertFalse(Files.exists(root.resolve("missing.json")));
    }

    @Test
    void writeCreatesOnlyTheDirectConfigRoot() throws Exception {
        Path root = tempDir.resolve("new-config");

        SecureConfigStorage.writeUtf8Atomic(root, "settings.json", "{}", 1024);

        assertTrue(Files.isDirectory(root));
        assertEquals("{}", Files.readString(root.resolve("settings.json"), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsTraversalNestedPathsUnsafeNamesAndInvalidBudgets() throws Exception {
        Path root = configRoot();

        for (String unsafe : List.of(
                "../escape.json", "nested/settings.json", "bad..json", "CON.json", "x\\y.json")) {
            assertFalse(SecureConfigStorage.isSafeTarget(root, unsafe), unsafe);
            assertThrows(IOException.class,
                    () -> SecureConfigStorage.writeUtf8Atomic(root, unsafe, "x", 1024));
        }
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "ok.json", "x", 0));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(
                        root, "ok.json", "x", 16L * 1024L * 1024L + 1L));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "ok.json", -1));
        assertFalse(Files.exists(tempDir.resolve("escape.json")));
    }

    @Test
    void oversizedContentPreservesExistingFile() throws Exception {
        Path root = configRoot();
        Path target = root.resolve("settings.json");
        Files.writeString(target, "old", StandardCharsets.UTF_8);

        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "settings.json", "0123456789", 4));

        assertEquals("old", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void oversizedFileIsRejectedDuringRead() throws Exception {
        Path root = configRoot();
        Files.write(root.resolve("large.json"), new byte[(int) RuntimeSecurityPolicy.MAX_CONFIG_BYTES + 1]);

        IOException failure = assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(
                        root, "large.json", RuntimeSecurityPolicy.MAX_CONFIG_BYTES));
        assertTrue(failure.getMessage().contains("size budget"));
    }

    @Test
    void malformedUtf8IsRejected() throws Exception {
        Path root = configRoot();
        Files.write(root.resolve("malformed.json"), new byte[]{(byte) 0xC3, 0x28});

        IOException failure = assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "malformed.json", 1024));
        assertTrue(failure.getMessage().contains("strict UTF-8"));
    }

    @Test
    void rejectsDirectoryAndSymbolicLinkTargets() throws Exception {
        Path root = configRoot();
        Files.createDirectory(root.resolve("directory.json"));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "directory.json", "x", 1024));

        Path outside = tempDir.resolve("outside.json");
        Files.writeString(outside, "outside", StandardCharsets.UTF_8);
        Path link = root.resolve("linked.json");
        if (tryCreateSymbolicLink(link, outside)) {
            assertFalse(SecureConfigStorage.isSafeTarget(root, "linked.json"));
            assertThrows(IOException.class,
                    () -> SecureConfigStorage.writeUtf8Atomic(root, "linked.json", "x", 1024));
            assertEquals("outside", Files.readString(outside, StandardCharsets.UTF_8));
        }
    }

    @Test
    void injectedFailureAfterStagingPreservesOldFileAndCleansTemporaryFile() throws Exception {
        Path root = configRoot();
        Path target = root.resolve("settings.json");
        Files.writeString(target, "old", StandardCharsets.UTF_8);

        IOException failure = assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "settings.json",
                "new",
                1024,
                (stagedFile, expectedTarget) -> {
                    assertTrue(Files.isRegularFile(stagedFile));
                    assertEquals(target.toRealPath(), expectedTarget);
                    throw new IOException("injected interruption");
                }));

        assertEquals("injected interruption", failure.getMessage());
        assertEquals("old", Files.readString(target, StandardCharsets.UTF_8));
        try (var files = Files.list(root)) {
            List<Path> leftovers = files
                    .filter(path -> path.getFileName().toString().startsWith(".chise-config-"))
                    .toList();
            assertTrue(leftovers.isEmpty(), () -> "temporary files leaked: " + leftovers);
        }
    }

    @Test
    void rejectsSymlinkConfigRootWhenSupported() throws Exception {
        Path realRoot = tempDir.resolve("real-config");
        Files.createDirectory(realRoot);
        Path linkedRoot = tempDir.resolve("linked-config");
        if (!tryCreateSymbolicLink(linkedRoot, realRoot)) return;

        assertFalse(SecureConfigStorage.isSafeTarget(linkedRoot, "settings.json"));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(linkedRoot, "settings.json", "x", 1024));
    }

    @Test
    void refusesToCreateConfigRootThroughASymlinkParent() throws Exception {
        Path outside = tempDir.resolve("outside-dir");
        Files.createDirectory(outside);
        Path linkedParent = tempDir.resolve("linked-parent");
        if (!tryCreateSymbolicLink(linkedParent, outside)) return;

        Path requestedRoot = linkedParent.resolve("config");
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(requestedRoot, "settings.json", "x", 1024));
        assertFalse(Files.exists(outside.resolve("config")));
    }

    private Path configRoot() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectories(root);
        return root;
    }

    private static boolean tryCreateSymbolicLink(Path link, Path target) {
        try {
            Path relativeTarget = link.getParent().relativize(target.toAbsolutePath().normalize());
            Files.createSymbolicLink(link, relativeTarget);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            return false;
        }
    }
}
