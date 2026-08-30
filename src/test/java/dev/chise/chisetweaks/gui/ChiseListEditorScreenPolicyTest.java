package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseListEditorScreenPolicyTest {
    @Test
    void blankInputCannotBeSubmitted() {
        assertFalse(ChiseListEditorScreen.canSubmitOreEntry("  ", List.of()));
    }

    @Test
    void newEntryIsAllowedBelowTheLimit() {
        assertTrue(ChiseListEditorScreen.canSubmitOreEntry(
                "examplemod:new_ore",
                List.of(new OreHighlightCompatibilityConfig.Entry(
                        "examplemod:existing_ore", OreHighlightStyle.GENERIC))));
    }

    @Test
    void newEntryIsBlockedAtTheLimitButExistingEntryCanStillBeUpdated() {
        ArrayList<OreHighlightCompatibilityConfig.Entry> entries = new ArrayList<>();
        for (int index = 0; index < OreHighlightCompatibilityConfig.MAX_ENTRIES; index++) {
            entries.add(new OreHighlightCompatibilityConfig.Entry(
                    "examplemod:ore_" + index,
                    OreHighlightStyle.GENERIC));
        }

        assertFalse(ChiseListEditorScreen.canSubmitOreEntry("examplemod:new_ore", entries));
        assertTrue(ChiseListEditorScreen.canSubmitOreEntry(" EXAMPLEMOD:ORE_255 ", entries));
    }
}
