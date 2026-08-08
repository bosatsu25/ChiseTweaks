package dev.chise.chisetweaks.core.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Bounded SHA-256 helpers for small local files and staged imports. */
public final class BoundedSha256 {
    private static final int BUFFER_BYTES = 8 * 1024;

    private BoundedSha256() {}

    public static byte[] digest(byte[] payload) {
        MessageDigest digest = newDigest();
        digest.update(payload == null ? new byte[0] : payload);
        return digest.digest();
    }

    public static byte[] digest(Path path, long maxBytes) throws IOException {
        if (path == null || maxBytes <= 0 || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("digest target is not a safe regular file");
        }
        long initialSize = Files.size(path);
        if (initialSize > maxBytes) throw new IOException("digest target exceeds size budget");

        MessageDigest digest = newDigest();
        byte[] buffer = new byte[BUFFER_BYTES];
        long total = 0;
        try (InputStream input = Files.newInputStream(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            while (true) {
                int read = input.read(buffer);
                if (read < 0) break;
                total += read;
                if (total > maxBytes) throw new IOException("digest target grew beyond size budget");
                digest.update(buffer, 0, read);
            }
        }
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) != total || total != initialSize) {
            throw new IOException("digest target changed while hashing");
        }
        return digest.digest();
    }

    public static boolean matches(Path path, byte[] expected, long maxBytes) throws IOException {
        return expected != null && MessageDigest.isEqual(expected, digest(path, maxBytes));
    }

    public static boolean equalsDigest(byte[] first, byte[] second) {
        return first != null && second != null && MessageDigest.isEqual(first, second);
    }

    public static String hex(byte[] digest) {
        return HexFormat.of().formatHex(Arrays.copyOf(digest, digest.length));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
