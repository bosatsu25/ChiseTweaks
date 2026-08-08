package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ConfigListPolicyTest {
    @Test
    void listIsTrimmedDeduplicatedAndBounded() {
        ArrayList<String> input = new ArrayList<>();
        input.add("  minecraft:stone  ");
        input.add("minecraft:stone");
        for (int index = 0; index < 600; index++) input.add("minecraft:block_" + index);

        List<String> sanitized = ConfigListPolicy.sanitize(input);

        assertEquals(ConfigListPolicy.MAX_ENTRIES, sanitized.size());
        assertEquals("minecraft:stone", sanitized.getFirst());
        assertEquals(sanitized.size(), sanitized.stream().distinct().count());
    }

    @Test
    void nullBlankControlBidiAndOversizedEntriesAreRejected() {
        String tooLong = "x".repeat(ConfigListPolicy.MAX_ENTRY_CHARS + 1);
        List<String> sanitized = ConfigListPolicy.sanitize(List.of(
                "",
                "   ",
                "bad\nvalue",
                "bad\u202Evalue",
                tooLong,
                "minecraft:glass"));

        assertEquals(List.of("minecraft:glass"), sanitized);
    }

    @Test
    void nullInputBecomesImmutableEmptyList() {
        List<String> sanitized = ConfigListPolicy.sanitize(null);
        assertTrue(sanitized.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> sanitized.add("x"));
    }
}
