package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ResourcePackSelectionPolicyTest {
    @Test
    void enablingAndDisablingOnePackPreservesOtherSelectionsAndOrder() {
        List<String> base = List.of("vanilla", "file/custom.zip", "chisetweaks:chise_white_concrete_visibility");
        assertEquals(
                List.of(
                        "vanilla",
                        "file/custom.zip",
                        "chisetweaks:chise_white_concrete_visibility",
                        "chisetweaks:chise_chest_visibility"),
                ResourcePackSelectionPolicy.withPack(
                        base, "chisetweaks:chise_chest_visibility", true));
        assertEquals(
                List.of("vanilla", "file/custom.zip", "chisetweaks:chise_white_concrete_visibility"),
                ResourcePackSelectionPolicy.withPack(
                        List.of(
                                "vanilla",
                                "file/custom.zip",
                                "chisetweaks:chise_white_concrete_visibility",
                                "chisetweaks:chise_chest_visibility"),
                        "chisetweaks:chise_chest_visibility",
                        false));
    }

    @Test
    void transitionsAreIdempotentAndDeduplicateMalformedInput() {
        assertEquals(
                List.of("vanilla", "chisetweaks:chise_chest_visibility"),
                ResourcePackSelectionPolicy.withPack(
                        List.of("vanilla", "chisetweaks:chise_chest_visibility", "vanilla"),
                        "chisetweaks:chise_chest_visibility",
                        true));
        assertEquals(
                List.of("vanilla"),
                ResourcePackSelectionPolicy.withPack(
                        List.of("vanilla"),
                        "chisetweaks:chise_chest_visibility",
                        false));
    }

    @Test
    void blankOrMissingPackIdsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> ResourcePackSelectionPolicy.withPack(List.of(), null, true));
        assertThrows(IllegalArgumentException.class,
                () -> ResourcePackSelectionPolicy.withPack(List.of(), "   ", true));
    }
}
