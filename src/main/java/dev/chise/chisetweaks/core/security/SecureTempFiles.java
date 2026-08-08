package dev.chise.chisetweaks.core.security;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

/** Cross-platform temporary-file helper with owner-only POSIX permissions when supported. */
public final class SecureTempFiles {
    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private SecureTempFiles() {}

    public static Path createOwnerOnly(Path directory, String prefix, String suffix) throws IOException {
        if (directory == null || Files.isSymbolicLink(directory)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("temporary-file directory is unsafe");
        }
        Path temp;
        if (Files.getFileStore(directory).supportsFileAttributeView(PosixFileAttributeView.class)) {
            temp = Files.createTempFile(directory, prefix, suffix,
                    PosixFilePermissions.asFileAttribute(OWNER_ONLY));
            Set<PosixFilePermission> actual = Files.getPosixFilePermissions(temp, LinkOption.NOFOLLOW_LINKS);
            boolean hasNonOwnerPermission = false;
            for (PosixFilePermission permission : actual) {
                if (isNonOwner(permission)) {
                    hasNonOwnerPermission = true;
                    break;
                }
            }
            if (!OWNER_ONLY.containsAll(actual) || hasNonOwnerPermission) {
                Files.deleteIfExists(temp);
                throw new IOException("temporary file permissions are too broad");
            }
        } else {
            temp = Files.createTempFile(directory, prefix, suffix);
        }
        if (Files.isSymbolicLink(temp) || !Files.isRegularFile(temp, LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(temp);
            throw new IOException("temporary path is not a regular file");
        }
        return temp;
    }

    public static void forceDirectoryBestEffort(Path directory) {
        if (directory == null) return;
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // Directory fsync is not supported on every Java/filesystem combination.
        }
    }

    private static boolean isNonOwner(PosixFilePermission permission) {
        return permission == PosixFilePermission.GROUP_READ
                || permission == PosixFilePermission.GROUP_WRITE
                || permission == PosixFilePermission.GROUP_EXECUTE
                || permission == PosixFilePermission.OTHERS_READ
                || permission == PosixFilePermission.OTHERS_WRITE
                || permission == PosixFilePermission.OTHERS_EXECUTE;
    }
}
