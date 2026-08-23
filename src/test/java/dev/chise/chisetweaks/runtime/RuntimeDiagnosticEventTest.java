package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeDiagnosticEventTest {
    @Test
    void wireNamesAreUniqueSingleTokenIdentifiers() {
        Set<String> unique = new HashSet<>();
        for (RuntimeDiagnosticEvent event : RuntimeDiagnosticEvent.values()) {
            assertFalse(event.wireName().isBlank());
            assertFalse(event.wireName().contains(" "));
            assertFalse(event.wireName().contains("="));
            assertTrue(unique.add(event.wireName()), event.wireName());
        }
    }
}
