package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.security.BoundedSha256;
import dev.chise.chisetweaks.core.security.SecureTempFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class CryptoAndTempFileSecurityTest {
    @TempDir
    Path tempDir;

    @Test
    void sha256UsesKnownVectorsAndNullPayloadIsEmptyPayload() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                BoundedSha256.hex(BoundedSha256.digest("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                BoundedSha256.hex(BoundedSha256.digest((byte[]) null)));
    }

    @Test
    void boundedFileHashAcceptsExactBudgetAndRejectsOversizedOrUnsafeTargets() throws Exception {
        Path file = tempDir.resolve("payload.bin");
        byte[] payload = new byte[64 * 1024];
        for (int index = 0; index < payload.length; index++) payload[index] = (byte) index;
        Files.write(file, payload);

        byte[] digest = BoundedSha256.digest(file, payload.length);
        assertTrue(BoundedSha256.matches(file, digest, payload.length));
        assertFalse(BoundedSha256.matches(file, new byte[32], payload.length));
        assertThrows(IOException.class, () -> BoundedSha256.digest(file, payload.length - 1L));
        assertThrows(IOException.class, () -> BoundedSha256.digest(file, 0));
        assertThrows(IOException.class, () -> BoundedSha256.digest(tempDir, 1024));
        assertThrows(IOException.class, () -> BoundedSha256.digest(null, 1024));
        assertFalse(BoundedSha256.equalsDigest(null, digest));
        assertFalse(BoundedSha256.equalsDigest(digest, null));
        assertTrue(BoundedSha256.equalsDigest(digest, digest.clone()));
    }

    @Test
    void boundedFileHashRejectsSymbolicLinksWhenSupported() throws Exception {
        Path target = tempDir.resolve("target.bin");
        Files.writeString(target, "target");
        Path link = tempDir.resolve("link.bin");
        if (!tryCreateSymbolicLink(link, target.getFileName())) return;

        assertThrows(IOException.class, () -> BoundedSha256.digest(link, 1024));
    }

    @Test
    void secureTempFileIsRegularAndOwnerOnlyOnPosixFilesystems() throws Exception {
        Path file = SecureTempFiles.createOwnerOnly(tempDir, "chi", ".tmp");
        try {
            assertTrue(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS));
            assertFalse(Files.isSymbolicLink(file));
            if (Files.getFileStore(tempDir).supportsFileAttributeView(PosixFileAttributeView.class)) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
                assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
                assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
                assertFalse(permissions.stream().anyMatch(CryptoAndTempFileSecurityTest::isNonOwnerPermission));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void secureTempFileRejectsMissingFilesAndSymlinkDirectories() throws Exception {
        assertThrows(IOException.class, () -> SecureTempFiles.createOwnerOnly(null, "chi", ".tmp"));
        Path regularFile = tempDir.resolve("not-a-directory");
        Files.writeString(regularFile, "x");
        assertThrows(IOException.class, () -> SecureTempFiles.createOwnerOnly(regularFile, "chi", ".tmp"));

        Path realDirectory = tempDir.resolve("real-dir");
        Files.createDirectory(realDirectory);
        Path linkDirectory = tempDir.resolve("link-dir");
        if (tryCreateSymbolicLink(linkDirectory, realDirectory.getFileName())) {
            assertThrows(IOException.class, () -> SecureTempFiles.createOwnerOnly(linkDirectory, "chi", ".tmp"));
        }

        assertDoesNotThrow(() -> SecureTempFiles.forceDirectoryBestEffort(tempDir));
        assertDoesNotThrow(() -> SecureTempFiles.forceDirectoryBestEffort(null));
    }

    private static boolean tryCreateSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            return false;
        }
    }

    private static boolean isNonOwnerPermission(PosixFilePermission permission) {
        return permission == PosixFilePermission.GROUP_READ
                || permission == PosixFilePermission.GROUP_WRITE
                || permission == PosixFilePermission.GROUP_EXECUTE
                || permission == PosixFilePermission.OTHERS_READ
                || permission == PosixFilePermission.OTHERS_WRITE
                || permission == PosixFilePermission.OTHERS_EXECUTE;
    }
}
