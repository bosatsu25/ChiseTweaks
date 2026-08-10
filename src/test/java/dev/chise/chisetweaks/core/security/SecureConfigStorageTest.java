package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SecureConfigStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void missingRootAndMissingFileAreNormalEmptyReadStates() throws IOException {
        Path root = tempDir.resolve("config");

        assertTrue(SecureConfigStorage.readUtf8(root, "settings.json").isEmpty());
        Files.createDirectory(root);
        assertTrue(SecureConfigStorage.readUtf8(root, "settings.json").isEmpty());
        assertTrue(SecureConfigStorage.isSafeTarget(root, "settings.json"));
    }

    @Test
    void atomicRoundTripPreservesStrictUtf8AndCleansStagingFiles() throws IOException {
        Path root = tempDir.resolve("config");
        String payload = "鉱石ハイライト 🚀\nLava Analyzer";

        SecureConfigStorage.writeUtf8Atomic(root, "visual.json", payload);

        assertEquals(payload, SecureConfigStorage.readUtf8(root, "visual.json").orElseThrow());
        assertEquals(List.of("visual.json"), Files.list(root)
                .map(path -> path.getFileName().toString())
                .sorted()
                .toList());
    }

    @Test
    void sizeBudgetUsesInclusiveBoundaryAndRejectsInvalidBudgets() throws IOException {
        Path root = tempDir.resolve("config");

        SecureConfigStorage.writeUtf8Atomic(root, "budget.json", "1234", 4);
        assertEquals("1234", SecureConfigStorage.readUtf8(root, "budget.json", 4).orElseThrow());
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "too-large.json", "12345", 4));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "budget.json", 3));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "budget.json", 0));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "budget.json", "x", 16L * 1024L * 1024L + 1));
    }

    @Test
    void malformedUtf8IsRejectedInsteadOfBeingReplacementDecoded() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectory(root);
        Files.write(root.resolve("broken.json"), new byte[] {(byte) 0xC3, 0x28});

        assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "broken.json", 16));
    }

    @Test
    void unsafeLeafNamesAndNonRegularTargetsAreRejected() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectory(root);
        Files.createDirectory(root.resolve("directory.json"));

        for (String unsafe : List.of(
                "../escape.json",
                "nested/config.json",
                "nested\\config.json",
                "CON.json",
                "trailing.json ",
                "bad?.json")) {
            assertThrows(IOException.class,
                    () -> SecureConfigStorage.writeUtf8Atomic(root, unsafe, "{}"), unsafe);
            assertFalse(SecureConfigStorage.isSafeTarget(root, unsafe), unsafe);
        }
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "directory.json", "{}"));
        assertFalse(SecureConfigStorage.isSafeTarget(root, "directory.json"));
    }

    @Test
    void symbolicLinkTargetCannotRedirectReadsOrWritesOutsideConfigRoot() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectory(root);
        Path outside = tempDir.resolve("outside.json");
        Files.writeString(outside, "outside", StandardCharsets.UTF_8);
        Path link = root.resolve("visual.json");
        assumeSymlinkAvailable(link, outside);

        assertThrows(IOException.class, () -> SecureConfigStorage.readUtf8(root, "visual.json"));
        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(root, "visual.json", "replacement"));
        assertEquals("outside", Files.readString(outside, StandardCharsets.UTF_8));
        assertFalse(SecureConfigStorage.isSafeTarget(root, "visual.json"));
    }

    @Test
    void symbolicLinkRootIsRejected() throws IOException {
        Path realRoot = tempDir.resolve("real-config");
        Files.createDirectory(realRoot);
        Path linkedRoot = tempDir.resolve("linked-config");
        assumeSymlinkAvailable(linkedRoot, realRoot);

        assertThrows(IOException.class, () -> SecureConfigStorage.readUtf8(linkedRoot, "visual.json"));
        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(linkedRoot, "visual.json", "{}"));
        assertFalse(SecureConfigStorage.isSafeTarget(linkedRoot, "visual.json"));
    }

    @Test
    void failedAtomicWriteLeavesExistingConfigUntouchedAndRemovesStageFile() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectory(root);
        Path target = root.resolve("visual.json");
        Files.writeString(target, "original", StandardCharsets.UTF_8);

        IOException failure = assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "visual.json",
                "replacement",
                128,
                (staged, ignoredTarget) -> {
                    assertTrue(Files.isRegularFile(staged));
                    throw new IOException("fault injection");
                }));

        assertEquals("fault injection", failure.getMessage());
        assertEquals("original", Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(Files.list(root).anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }

    @Test
    void targetIsRevalidatedAfterStagedContentVerification() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectory(root);
        Path target = root.resolve("visual.json");
        Files.writeString(target, "original", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "visual.json",
                "replacement",
                128,
                (staged, checkedTarget) -> {
                    assertTrue(Files.isRegularFile(staged));
                    Files.delete(checkedTarget);
                    Files.createDirectory(checkedTarget);
                }));

        assertTrue(Files.isDirectory(target));
        assertFalse(Files.list(root).anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }

    @Test
    void nullRootAndNullFilenameFailClosed() {
        assertThrows(IOException.class, () -> SecureConfigStorage.readUtf8(null, "visual.json"));
        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(null, "visual.json", "{}"));
        assertFalse(SecureConfigStorage.isSafeTarget(null, "visual.json"));

        assertThrows(IOException.class, () -> SecureConfigStorage.readUtf8(tempDir, null));
        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(tempDir, null, "{}"));
        assertFalse(SecureConfigStorage.isSafeTarget(tempDir, null));
    }

    private static void assumeSymlinkAvailable(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | SecurityException unavailable) {
            assumeTrue(false, "symbolic links are unavailable: " + unavailable.getMessage());
        } catch (IOException unavailable) {
            assumeTrue(false, "symbolic links are unavailable: " + unavailable.getMessage());
        }
    }
}
