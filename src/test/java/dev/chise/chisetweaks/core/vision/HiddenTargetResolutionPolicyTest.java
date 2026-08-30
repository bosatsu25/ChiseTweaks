package dev.chise.chisetweaks.core.vision;

import org.junit.jupiter.api.Test;

import static dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target.HIDDEN_BLUE_ICE;
import static dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target.HIDDEN_DEAD_CORAL;
import static dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target.HIDDEN_POWDER_SNOW;
import static dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target.HIDDEN_SCULK_CATALYST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class HiddenTargetResolutionPolicyTest {
    @Test
    void resolvesEverySupportedHiddenTargetFamily() {
        assertEquals(HIDDEN_BLUE_ICE,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:blue_ice"));
        assertEquals(HIDDEN_POWDER_SNOW,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:powder_snow"));
        assertEquals(HIDDEN_SCULK_CATALYST,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:sculk_catalyst"));
        assertEquals(HIDDEN_DEAD_CORAL,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:dead_fire_coral_block"));
        assertEquals(HIDDEN_DEAD_CORAL,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:dead_brain_coral_wall_fan"));
    }

    @Test
    void normalizesInputAndRejectsUnsupportedBlocks() {
        assertEquals(HIDDEN_BLUE_ICE,
                VisualTargetSelectionPolicy.hiddenTargetForBlockId(" MINECRAFT:BLUE_ICE "));
        assertNull(VisualTargetSelectionPolicy.hiddenTargetForBlockId("minecraft:diamond_ore"));
        assertNull(VisualTargetSelectionPolicy.hiddenTargetForBlockId(null));
    }
}
