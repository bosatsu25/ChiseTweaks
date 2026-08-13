package dev.chise.chisetweaks.core.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModdedOreIdPolicyTest {
    @Test
    void acceptsOnlyNonVanillaOreSuffixes() {
        assertTrue(ModdedOreIdPolicy.looksLikeOre("example:tin_ore"));
        assertTrue(ModdedOreIdPolicy.looksLikeOre("  Example:Deep_Tin_Ore  "));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("minecraft:diamond_ore"));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("example:ore"));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("example:ore_block"));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("example:tin_ore_block"));
    }

    @Test
    void malformedValuesFailClosed() {
        assertFalse(ModdedOreIdPolicy.looksLikeOre(null));
        assertFalse(ModdedOreIdPolicy.looksLikeOre(""));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("   "));
        assertFalse(ModdedOreIdPolicy.looksLikeOre(":tin_ore"));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("example:"));
        assertFalse(ModdedOreIdPolicy.looksLikeOre("tin_ore"));
    }

    @Test
    void normalizationIsNullSafe() {
        assertEquals("example:tin_ore", ModdedOreIdPolicy.normalize(" Example:TIN_ORE "));
        assertEquals("", ModdedOreIdPolicy.normalize(null));
    }
}
