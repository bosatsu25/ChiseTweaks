package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeDiagnosticDetailTest {
    @Test
    void rendersStableKeyValueToken() {
        assertEquals(
                "componentId=ancient_debris_analyzer",
                new RuntimeDiagnosticDetail("componentId", "ancient_debris_analyzer").toLogToken());
    }

    @Test
    void normalizesWhitespaceEqualsAndControlCharacters() {
        RuntimeDiagnosticDetail detail = new RuntimeDiagnosticDetail(
                "reason",
                " reload = failed\nnow ");
        assertEquals("reload___failed_now", detail.value());
    }

    @Test
    void blankValuesBecomeExplicitNone() {
        assertEquals("none", new RuntimeDiagnosticDetail("reason", "   ").value());
    }

    @Test
    void valuesAreBoundedToOneHundredTwentyEightCharacters() {
        RuntimeDiagnosticDetail detail = new RuntimeDiagnosticDetail("reason", "x".repeat(256));
        assertEquals(128, detail.value().length());
        assertTrue(detail.value().chars().allMatch(value -> value == 'x'));
    }

    @Test
    void rejectsUnsafeOrMissingKeysAndValues() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeDiagnosticDetail("bad key", "value"));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeDiagnosticDetail("1bad", "value"));
        assertThrows(NullPointerException.class, () -> new RuntimeDiagnosticDetail(null, "value"));
        assertThrows(NullPointerException.class, () -> new RuntimeDiagnosticDetail("reason", null));
    }

    @Test
    void factoryUsesStableStringRepresentation() {
        assertEquals("selectionChanged=true", RuntimeDiagnosticDetail.of("selectionChanged", true).toLogToken());
    }
}
