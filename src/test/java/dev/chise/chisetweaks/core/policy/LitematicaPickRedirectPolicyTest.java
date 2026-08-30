package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class LitematicaPickRedirectPolicyTest {
    @Test
    void resolvesFirstValidMappingForSchematicBlock() {
        assertEquals("minecraft:dirt", LitematicaPickRedirectPolicy.replacementFor(
                "minecraft:farmland",
                List.of(
                        "broken",
                        "minecraft:farmland,minecraft:dirt",
                        "minecraft:farmland,minecraft:coarse_dirt")));
    }

    @Test
    void malformedAndUnknownSourceStayNeutral() {
        assertEquals("", LitematicaPickRedirectPolicy.replacementFor(
                "minecraft:stone",
                List.of("minecraft:farmland,minecraft:dirt", "a:b:c,d:e")));
        assertEquals("", LitematicaPickRedirectPolicy.replacementFor(null, List.of()));
        assertEquals("", LitematicaPickRedirectPolicy.replacementFor("minecraft:stone", null));
        assertNull(LitematicaPickRedirectPolicy.parse("minecraft:stone"));
        assertNull(LitematicaPickRedirectPolicy.parse("MINECRAFT:STONE,minecraft:dirt?"));
    }

    @Test
    void spacesAreNormalizedWithoutAcceptingInvalidIdentifiers() {
        assertEquals("minecraft:dirt", LitematicaPickRedirectPolicy.replacementFor(
                "MINECRAFT:FARMLAND",
                List.of(" minecraft:farmland , minecraft:dirt ")));
    }
}
