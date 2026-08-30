package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaIdListPolicyTest {
    @Test
    void noneAllowsValidIdsAndRejectsMissingIds() {
        assertTrue(MasaIdListPolicy.allows("minecraft:dirt", MasaIdListPolicy.NONE, List.of(), List.of()));
        assertFalse(MasaIdListPolicy.allows("", MasaIdListPolicy.NONE, List.of(), List.of()));
        assertTrue(MasaIdListPolicy.allows("minecraft:dirt", 99, List.of(), List.of()));
    }

    @Test
    void whitelistOnlyAllowsListedIds() {
        assertTrue(MasaIdListPolicy.allows(
                "minecraft:golden_carrot", MasaIdListPolicy.WHITELIST,
                List.of(" minecraft:golden_carrot "), List.of()));
        assertFalse(MasaIdListPolicy.allows(
                "minecraft:stone", MasaIdListPolicy.WHITELIST,
                List.of("minecraft:golden_carrot"), List.of()));
    }

    @Test
    void blacklistOnlyRejectsListedIds() {
        assertFalse(MasaIdListPolicy.allows(
                "minecraft:ender_chest", MasaIdListPolicy.BLACKLIST,
                List.of(), List.of("minecraft:ender_chest")));
        assertTrue(MasaIdListPolicy.allows(
                "minecraft:stone", MasaIdListPolicy.BLACKLIST,
                List.of(), List.of("minecraft:ender_chest")));
    }
}
