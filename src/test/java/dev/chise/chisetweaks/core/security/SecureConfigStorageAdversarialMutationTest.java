package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SecureConfigStorageAdversarialMutationTest {
    @TempDir
    Path tempDir;

    @Test
    void readingADirectoryFailsWithTheStorageBoundaryError() throws Exception {
        Path root = root();
        Files.createDirectory(root.resolve("directory.json"));

        IOException failure = assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "directory.json", 1024));
        assertEquals("Config target is not a readable regular file", failure.getMessage());
    }

    @Test
    void existingNonRegularTargetIsRejectedBeforeTheCheckpoint() throws Exception {
        Path root = root();
        Files.createDirectory(root.resolve("settings.json"));
        AtomicBoolean checkpointReached = new AtomicBoolean(false);

        IOException failure = assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "settings.json",
                "payload",
                1024,
                (stagedFile, target) -> checkpointReached.set(true)));

        assertEquals("Refusing to replace a non-regular config target", failure.getMessage());
        assertFalse(checkpointReached.get());
    }

    @Test
    void targetBecomingNonRegularDuringWriteIsRejectedOnRevalidation() throws Exception {
        Path root = root();

        IOException failure = assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "settings.json",
                "payload",
                1024,
                (stagedFile, target) -> Files.createDirectory(target)));

        assertEquals("Refusing to replace a non-regular config target", failure.getMessage());
        assertTrue(Files.isDirectory(root.resolve("settings.json")));
    }

    @Test
    void stagedFileTamperingIsCaughtByThePostReplacementVerification() throws Exception {
        Path root = root();

        IOException failure = assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(
                root,
                "settings.json",
                "payload",
                1024,
                (stagedFile, target) -> Files.writeString(
                        stagedFile, "tampered-payload", StandardCharsets.UTF_8))));

        assertEquals("Config replacement postcondition failed", failure.getMessage());
        assertEquals("tampered-payload",
                Files.readString(root.resolve("settings.json"), StandardCharsets.UTF_8));
    }

    @Test
    void identityHelperRejectsNonRegularFiles() throws Exception {
        Path root = root();
        Path directory = root.resolve("directory");
        Files.createDirectory(directory);

        Method identity = SecureConfigStorage.class.getDeclaredMethod("identity", Path.class);
        identity.setAccessible(true);
        InvocationTargetException wrapper = assertThrows(
                InvocationTargetException.class, () -> identity.invoke(null, directory));
        assertTrue(wrapper.getCause() instanceof IOException);
        assertEquals("Config target is not a regular file", wrapper.getCause().getMessage());
    }

    private Path root() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectories(root);
        return root;
    }
}
