package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SecurityPrimitiveTest {
    @TempDir
    Path tempDir;

    @Test
    void strictUtf8RoundTripsMultilingualTextAndRejectsMalformedInput() throws IOException {
        String value = "鉱石ハイライト / Lava Analyzer / 🚀";
        byte[] encoded = StrictUtf8.encode(value);

        assertEquals(value, StrictUtf8.decode(encoded));
        assertThrows(IOException.class, () -> StrictUtf8.decode(new byte[] {(byte) 0xC3, 0x28}));
        assertThrows(IOException.class, () -> StrictUtf8.encode("bad\uD800text"));
        assertThrows(IOException.class, () -> StrictUtf8.decode(null));
        assertThrows(IOException.class, () -> StrictUtf8.encode(null));
    }

    @Test
    void sha256ByteAndFileDigestsMatchTheKnownVectorAndRespectBudget() throws IOException {
        byte[] payload = StrictUtf8.encode("abc");
        byte[] digest = BoundedSha256.digest(payload);
        Path file = tempDir.resolve("payload.bin");
        Files.write(file, payload);

        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                BoundedSha256.hex(digest));
        assertArrayEquals(digest, BoundedSha256.digest(file, payload.length));
        assertTrue(BoundedSha256.matches(file, digest, payload.length));
        assertTrue(BoundedSha256.equalsDigest(digest, digest.clone()));
        assertFalse(BoundedSha256.equalsDigest(digest, null));
        assertFalse(BoundedSha256.matches(file, null, payload.length));
        assertThrows(IOException.class, () -> BoundedSha256.digest(file, payload.length - 1));
        assertThrows(IOException.class, () -> BoundedSha256.digest(file, 0));
        assertThrows(IOException.class, () -> BoundedSha256.digest(null, payload.length));
    }

    @Test
    void boundedDigestRejectsSymbolicLinks() throws IOException {
        Path target = tempDir.resolve("target.bin");
        Files.writeString(target, "payload");
        Path link = tempDir.resolve("link.bin");
        assumeSymlinkAvailable(link, target);

        assertThrows(IOException.class, () -> BoundedSha256.digest(link, 64));
    }

    @Test
    void secureTempFileIsRegularAndOwnerOnlyWherePosixPermissionsExist() throws IOException {
        Path temp = SecureTempFiles.createOwnerOnly(tempDir, ".security-test-", ".tmp");

        assertTrue(Files.isRegularFile(temp, LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.isSymbolicLink(temp));
        if (Files.getFileStore(tempDir).supportsFileAttributeView(PosixFileAttributeView.class)) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(temp, LinkOption.NOFOLLOW_LINKS);
            assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
            assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
            assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
            assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
            assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
            assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
            assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
            assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
        }
    }

    @Test
    void secureTempFileRejectsMissingAndNonDirectoryRoots() throws IOException {
        Path fileRoot = tempDir.resolve("not-a-directory");
        Files.writeString(fileRoot, "x");

        assertThrows(IOException.class,
                () -> SecureTempFiles.createOwnerOnly(null, ".tmp-", ".part"));
        assertThrows(IOException.class,
                () -> SecureTempFiles.createOwnerOnly(fileRoot, ".tmp-", ".part"));
    }

    private static void assumeSymlinkAvailable(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | SecurityException | IOException unavailable) {
            assumeTrue(false, "symbolic links are unavailable: " + unavailable.getMessage());
        }
    }
}
