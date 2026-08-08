package dev.chise.chisetweaks.core.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * Single storage boundary for all ChiseTweaks config documents.
 *
 * <p>Callers provide a trusted config directory and one safe leaf filename. The storage layer owns
 * path confinement, symlink rejection, bounded strict-UTF-8 reads, staged writes, integrity checks,
 * and replacement semantics. Keeping these rules here prevents config implementations from
 * drifting to different security guarantees.</p>
 */
public final class SecureConfigStorage {
    private static final int READ_BUFFER_BYTES = 8 * 1024;
    private static final long MAX_ALLOWED_BUDGET_BYTES = 16L * 1024L * 1024L;

    private SecureConfigStorage() {}

    public static Optional<String> readUtf8(Path configRoot, String fileName) throws IOException {
        return readUtf8(configRoot, fileName, RuntimeSecurityPolicy.MAX_CONFIG_BYTES);
    }

    public static void writeUtf8Atomic(Path configRoot, String fileName, String content) throws IOException {
        writeUtf8Atomic(
                configRoot,
                fileName,
                content,
                RuntimeSecurityPolicy.MAX_CONFIG_BYTES,
                AtomicWriteCheckpoint.NONE);
    }

    static Optional<String> readUtf8(Path configRoot, String fileName, long maxBytes) throws IOException {
        validateBudget(maxBytes);
        Path normalizedRoot = normalizeRoot(configRoot);
        if (!Files.exists(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }

        Path root = requireSafeRoot(normalizedRoot, false);
        Path target = resolveSafeLeaf(root, fileName);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        if (!isRegularNoFollow(target) || !Files.isReadable(target)) {
            throw new IOException("Config target is not a readable regular file");
        }

        FileIdentity before = identity(target);
        if (before.size() > maxBytes) throw new IOException("Config exceeds size budget");

        int initialCapacity = (int) Math.min(before.size(), READ_BUFFER_BYTES);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[READ_BUFFER_BYTES];
        long total = 0;
        try (InputStream input = Files.newInputStream(
                target, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            while (true) {
                int read = input.read(buffer);
                if (read < 0) break;
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Config grew beyond size budget while reading");
                }
                output.write(buffer, 0, read);
            }
        }

        FileIdentity after = identity(target);
        byte[] bytes = output.toByteArray();
        if (!before.sameFile(after) || total != after.size()) {
            throw new IOException("Config changed while it was being read");
        }
        byte[] digest = BoundedSha256.digest(bytes);
        if (!BoundedSha256.matches(target, digest, maxBytes)) {
            throw new IOException("Config content changed after reading");
        }
        return Optional.of(StrictUtf8.decode(bytes));
    }

    static void writeUtf8Atomic(
            Path configRoot,
            String fileName,
            String content,
            long maxBytes) throws IOException {
        writeUtf8Atomic(configRoot, fileName, content, maxBytes, AtomicWriteCheckpoint.NONE);
    }

    static void writeUtf8Atomic(
            Path configRoot,
            String fileName,
            String content,
            long maxBytes,
            AtomicWriteCheckpoint checkpoint) throws IOException {
        validateBudget(maxBytes);
        byte[] payload = StrictUtf8.encode(content == null ? "" : content);
        if (payload.length > maxBytes) throw new IOException("Config exceeds size budget");

        Path normalizedRoot = normalizeRoot(configRoot);
        Path root = requireSafeRoot(normalizedRoot, true);
        Path target = resolveSafeLeaf(root, fileName);
        rejectNonRegularTarget(target);

        byte[] expectedDigest = BoundedSha256.digest(payload);
        Path staged = SecureTempFiles.createOwnerOnly(root, ".chise-config-", ".tmp");
        try {
            writeAndSynchronize(staged, payload);
            verifyFileContents(staged, payload.length, expectedDigest, maxBytes,
                    "Staged config integrity mismatch");
            checkpoint.afterStagedFileVerified(staged, target);

            Path revalidatedRoot = requireSafeRoot(normalizedRoot, false);
            if (!revalidatedRoot.equals(root)) {
                throw new IOException("Config root changed during write");
            }
            Path revalidatedTarget = resolveSafeLeaf(revalidatedRoot, fileName);
            if (!revalidatedTarget.equals(target)) {
                throw new IOException("Config target changed during write");
            }
            rejectNonRegularTarget(target);

            moveReplacing(staged, target);
            verifyFileContents(target, payload.length, expectedDigest, maxBytes,
                    "Config replacement postcondition failed");
            SecureTempFiles.forceDirectoryBestEffort(root);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    static boolean isSafeTarget(Path configRoot, String fileName) {
        try {
            Path normalizedRoot = normalizeRoot(configRoot);
            if (!Files.exists(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) return false;
            Path root = requireSafeRoot(normalizedRoot, false);
            Path target = resolveSafeLeaf(root, fileName);
            return !Files.exists(target, LinkOption.NOFOLLOW_LINKS) || isRegularNoFollow(target);
        } catch (IOException unsafe) {
            return false;
        }
    }

    private static Path normalizeRoot(Path configRoot) throws IOException {
        if (configRoot == null) throw new IOException("Config root is missing");
        return configRoot.toAbsolutePath().normalize();
    }

    private static Path requireSafeRoot(Path normalizedRoot, boolean create) throws IOException {
        if (create && !Files.exists(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            Path parent = normalizedRoot.getParent();
            if (parent == null
                    || Files.isSymbolicLink(parent)
                    || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Config root parent is unsafe");
            }
            Files.createDirectory(normalizedRoot);
        }
        if (Files.isSymbolicLink(normalizedRoot)
                || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Config root is unsafe");
        }
        Path realRoot = normalizedRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (Files.isSymbolicLink(realRoot)
                || !Files.isDirectory(realRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Config root does not resolve to a normal directory");
        }
        return realRoot;
    }

    private static Path resolveSafeLeaf(Path root, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()
                || !RuntimeSecurityPolicy.isSafeLeafFileName(fileName)) {
            throw new IOException("Unsafe config filename");
        }

        Path leaf;
        try {
            leaf = Path.of(fileName);
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid config filename", invalid);
        }
        if (leaf.isAbsolute() || leaf.getNameCount() != 1) {
            throw new IOException("Config filename must be one relative leaf");
        }

        Path target = root.resolve(leaf).normalize();
        if (!target.getParent().equals(root)) {
            throw new IOException("Config target escapes its root");
        }
        if (Files.isSymbolicLink(target)) {
            throw new IOException("Symbolic-link config target is not allowed");
        }
        return target;
    }

    private static void rejectNonRegularTarget(Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !isRegularNoFollow(target)) {
            throw new IOException("Refusing to replace a non-regular config target");
        }
    }

    private static void writeAndSynchronize(Path staged, byte[] payload) throws IOException {
        try (FileChannel channel = FileChannel.open(
                staged, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer remaining = ByteBuffer.wrap(payload);
            while (remaining.hasRemaining()) channel.write(remaining);
            channel.force(true);
        }
    }

    private static void verifyFileContents(
            Path file,
            int expectedSize,
            byte[] expectedDigest,
            long maxBytes,
            String failureMessage) throws IOException {
        if (!isRegularNoFollow(file)
                || Files.size(file) != expectedSize
                || !BoundedSha256.matches(file, expectedDigest, maxBytes)) {
            throw new IOException(failureMessage);
        }
    }

    private static void moveReplacing(Path staged, Path target) throws IOException {
        try {
            Files.move(staged, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isRegularNoFollow(Path path) {
        return !Files.isSymbolicLink(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }

    private static FileIdentity identity(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
            throw new IOException("Config target is not a regular file");
        }
        return new FileIdentity(
                attributes.fileKey(), attributes.size(), attributes.lastModifiedTime().toMillis());
    }

    private static void validateBudget(long maxBytes) throws IOException {
        if (maxBytes <= 0 || maxBytes > MAX_ALLOWED_BUDGET_BYTES) {
            throw new IOException("Invalid config size budget");
        }
    }

    @FunctionalInterface
    interface AtomicWriteCheckpoint {
        AtomicWriteCheckpoint NONE = (stagedFile, target) -> {};

        void afterStagedFileVerified(Path stagedFile, Path target) throws IOException;
    }

    private record FileIdentity(Object fileKey, long size, long modifiedMillis) {
        private boolean sameFile(FileIdentity other) {
            if (other == null || size != other.size) return false;
            if (fileKey != null && other.fileKey != null) return fileKey.equals(other.fileKey);
            return modifiedMillis == other.modifiedMillis;
        }
    }
}
