package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.integration.masa.MasaModAvailability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaGuideCatalogTest {
    @Test
    void guideAlwaysExplainsAllFiveMasaTargets() {
        var entries = MasaGuideCatalog.entries(
                new MasaModAvailability.Snapshot(false, true, false, true, false));
        assertEquals(5, entries.size());
        assertEquals("MaLiLib", entries.get(0).name());
        assertEquals("Litematica", entries.get(1).name());
        assertTrue(entries.get(1).installed());
        assertTrue(entries.get(3).installed());
        assertFalse(entries.get(4).installed());
        for (var entry : entries) {
            assertFalse(entry.summary().isBlank());
            assertFalse(entry.directions().isBlank());
        }
    }
}
