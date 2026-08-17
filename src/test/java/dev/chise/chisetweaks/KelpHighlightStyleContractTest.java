package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KelpHighlightStyleContractTest {
    @Test
    void kelpHighlightUsesRequestedMagentaAndOrangePalette() {
        assertEquals(0xFFFF4FD8, VisualAssistanceStylePolicy.KELP_MAGENTA);
        assertEquals(0xFFFF8A00, VisualAssistanceStylePolicy.KELP_ORANGE);
    }

    @Test
    void kelpAndKelpPlantUseTheSameHighVisibilityStyle() {
        var kelp = VisualAssistanceStylePolicy.styleFor(
                "minecraft:kelp", BlockInspectionCategory.KELP_HIGHLIGHT);
        var plant = VisualAssistanceStylePolicy.styleFor(
                "minecraft:kelp_plant", BlockInspectionCategory.KELP_HIGHLIGHT);

        assertEquals(VisualAssistanceStylePolicy.KELP_MAGENTA, kelp.argb());
        assertEquals(kelp, plant);
        assertEquals(85, kelp.priority());
        assertEquals(VisualAssistanceStylePolicy.Marker.CROSS, kelp.marker());
        assertTrue(kelp.visible());
    }
}
