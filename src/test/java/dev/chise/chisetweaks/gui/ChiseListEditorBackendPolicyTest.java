package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseListEditorBackendPolicyTest {
    @Test
    void sceneRuleModeCyclesThroughDisabledDenyAllow() {
        assertEquals(ChiseRuleMode.BLACKLIST, SceneFilterBackend.nextMode(ChiseRuleMode.NONE));
        assertEquals(ChiseRuleMode.WHITELIST, SceneFilterBackend.nextMode(ChiseRuleMode.BLACKLIST));
        assertEquals(ChiseRuleMode.NONE, SceneFilterBackend.nextMode(ChiseRuleMode.WHITELIST));
    }

    @Test
    void masaGuardModeCyclesThroughDisabledDenyAllow() {
        assertEquals(MasaIdListPolicy.BLACKLIST, MasaListBackend.nextMode(MasaIdListPolicy.NONE));
        assertEquals(MasaIdListPolicy.WHITELIST, MasaListBackend.nextMode(MasaIdListPolicy.BLACKLIST));
        assertEquals(MasaIdListPolicy.NONE, MasaListBackend.nextMode(MasaIdListPolicy.WHITELIST));
    }

    @Test
    void blankOreInputCannotBeSubmitted() {
        assertFalse(OreCompatibilityBackend.canSubmitEntry("  ", List.of()));
    }

    @Test
    void newOreEntryIsAllowedBelowTheLimit() {
        assertTrue(OreCompatibilityBackend.canSubmitEntry(
                "examplemod:new_ore",
                List.of(new OreHighlightCompatibilityConfig.Entry(
                        "examplemod:existing_ore", OreHighlightStyle.GENERIC))));
    }

    @Test
    void newOreEntryIsBlockedAtTheLimitButExistingEntryCanStillBeUpdated() {
        ArrayList<OreHighlightCompatibilityConfig.Entry> entries = new ArrayList<>();
        for (int index = 0; index < OreHighlightCompatibilityConfig.MAX_ENTRIES; index++) {
            entries.add(new OreHighlightCompatibilityConfig.Entry(
                    "examplemod:ore_" + index,
                    OreHighlightStyle.GENERIC));
        }

        assertFalse(OreCompatibilityBackend.canSubmitEntry("examplemod:new_ore", entries));
        assertTrue(OreCompatibilityBackend.canSubmitEntry(" EXAMPLEMOD:ORE_255 ", entries));
    }
}
