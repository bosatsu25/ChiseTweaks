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
    void targetDecisionTableKeepsSixEditorProfilesStable() {
        record Case(
                ChiseListEditorScreen.Target target,
                Class<?> backendType,
                boolean mode,
                boolean secondInput,
                boolean oreLayout) {}

        List<Case> cases = List.of(
                new Case(ChiseListEditorScreen.Target.BLOCK_FILTER, SceneFilterBackend.class, true, false, false),
                new Case(ChiseListEditorScreen.Target.ENTITY_FILTER, SceneFilterBackend.class, true, false, false),
                new Case(ChiseListEditorScreen.Target.ORE_COMPATIBILITY, OreCompatibilityBackend.class, false, false, true),
                new Case(ChiseListEditorScreen.Target.LITEMATICA_PICK_REDIRECT, MasaListBackend.class, false, true, false),
                new Case(ChiseListEditorScreen.Target.TWEAKERMORE_AUTO_PICK_GUARD, MasaListBackend.class, true, false, false),
                new Case(ChiseListEditorScreen.Target.TWEAKEROO_TOOL_SWITCH_GUARD, MasaListBackend.class, true, false, false));

        for (Case testCase : cases) {
            ChiseListEditorBackend backend = ChiseListEditorBackend.create(testCase.target());
            assertEquals(testCase.backendType(), backend.getClass(), testCase.target().name());
            assertEquals(testCase.mode(), backend.hasMode(), testCase.target().name());
            assertEquals(testCase.secondInput(), backend.hasSecondInput(), testCase.target().name());
            assertEquals(testCase.oreLayout(), backend.usesOreLayout(), testCase.target().name());
        }
    }

    @Test
    void nullTargetStillDefaultsToBlockFilter() {
        assertEquals(SceneFilterBackend.class, ChiseListEditorBackend.create(null).getClass());
    }

    @Test
    void masaAccessibilityTitleRemainsStableWhileDisplayTitleStaysSpecific() {
        ChiseListEditorBackend backend =
                ChiseListEditorBackend.create(ChiseListEditorScreen.Target.LITEMATICA_PICK_REDIRECT);
        assertEquals("Masa Integration", backend.screenTitle().getString());
        assertEquals("Litematica Pick Redirect", backend.title().getString());
    }

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
