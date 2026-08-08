package dev.chise.chisetweaks.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SecureConfigStorageMutationBoundaryTest {
    private static final long MAX_ALLOWED_BUDGET_BYTES = 16L * 1024L * 1024L;

    @TempDir
    Path tempDir;

    @Test
    void publicDefaultBudgetOverloadsRoundTripContent() throws Exception {
        Path root = root();
        SecureConfigStorage.writeUtf8Atomic(root, "default.json", "default-value");
        assertEquals("default-value",
                SecureConfigStorage.readUtf8(root, "default.json").orElseThrow());
    }

    @Test
    void exactSizeBudgetIsAcceptedForWriteAndRead() throws Exception {
        Path root = root();
        String content = "four";
        SecureConfigStorage.writeUtf8Atomic(root, "exact.json", content, 4);
        assertEquals(content, SecureConfigStorage.readUtf8(root, "exact.json", 4).orElseThrow());
        assertThrows(IOException.class,
                () -> SecureConfigStorage.readUtf8(root, "exact.json", 3));
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "too-small.json", content, 3));
    }

    @Test
    void budgetUpperBoundaryItselfRemainsValid() throws Exception {
        Path root = root();
        SecureConfigStorage.writeUtf8Atomic(root, "upper.json", "x", MAX_ALLOWED_BUDGET_BYTES);
        assertEquals("x", SecureConfigStorage.readUtf8(root, "upper.json", MAX_ALLOWED_BUDGET_BYTES).orElseThrow());
        assertThrows(IOException.class,
                () -> SecureConfigStorage.writeUtf8Atomic(root, "invalid.json", "x", MAX_ALLOWED_BUDGET_BYTES + 1));
    }

    @Test
    void safeTargetDistinguishesMissingRootRegularFileDirectoryAndUnsafeLeaf() throws Exception {
        Path missingRoot = tempDir.resolve("missing");
        assertFalse(SecureConfigStorage.isSafeTarget(missingRoot, "settings.json"));

        Path root = root();
        assertTrue(SecureConfigStorage.isSafeTarget(root, "not-created.json"));
        Files.writeString(root.resolve("regular.json"), "{}", StandardCharsets.UTF_8);
        assertTrue(SecureConfigStorage.isSafeTarget(root, "regular.json"));

        Files.createDirectory(root.resolve("directory.json"));
        assertFalse(SecureConfigStorage.isSafeTarget(root, "directory.json"));
        assertFalse(SecureConfigStorage.isSafeTarget(root, ""));
        assertFalse(SecureConfigStorage.isSafeTarget(root, null));
    }

    @Test
    void nullRootFailsClosedAcrossReadWriteAndProbe() {
        assertThrows(IOException.class, () -> SecureConfigStorage.readUtf8(null, "settings.json", 16));
        assertThrows(IOException.class, () -> SecureConfigStorage.writeUtf8Atomic(null, "settings.json", "{}", 16));
        assertFalse(SecureConfigStorage.isSafeTarget(null, "settings.json"));
    }

    private Path root() throws IOException {
        Path root = tempDir.resolve("config");
        Files.createDirectories(root);
        return root;
    }
}
