package dev.chise.chisetweaks.api.ore;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OreHighlightStyleTest {
    @Test
    void keysRoundTripAndRemainUnique() {
        Set<String> keys = new HashSet<>();
        for (OreHighlightStyle style : OreHighlightStyle.values()) {
            assertEquals(style, OreHighlightStyle.fromKey(style.key()));
            assertSame(style, OreHighlightStyle.fromKey("  " + style.key().toUpperCase() + "  "));
            assertTrue(keys.add(style.key()), style.key());
        }
    }

    @Test
    void unknownStylesFailClosed() {
        assertNull(OreHighlightStyle.fromKey(null));
        assertNull(OreHighlightStyle.fromKey(""));
        assertNull(OreHighlightStyle.fromKey("not-a-style"));
    }

    @Test
    void nextCyclesAcrossAllStyles() {
        OreHighlightStyle current = OreHighlightStyle.GENERIC;
        Set<OreHighlightStyle> visited = new HashSet<>();
        for (int index = 0; index < OreHighlightStyle.values().length; index++) {
            visited.add(current);
            current = current.next();
        }
        assertEquals(Set.of(OreHighlightStyle.values()), visited);
        assertSame(OreHighlightStyle.GENERIC, current);
    }
}
