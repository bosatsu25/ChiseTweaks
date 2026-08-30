package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HiddenBlockAnalyzerPalettePolicyTest {
    @Test
    void autoPalettePreservesPerTargetHiddenColors() {
        int powder = HiddenBlockAnalyzerPalettePolicy.colorForDistance(
                0.0, "minecraft:powder_snow", -1, 100);
        int ice = HiddenBlockAnalyzerPalettePolicy.colorForDistance(
                0.0, "minecraft:blue_ice", -1, 100);

        assertEquals(0xFFA68BFF, powder);
        assertEquals(0xFF8A76FF, ice);
    }

    @Test
    void customColorAndOpacityApplyToBothOutlineAndFace() {
        int outline = HiddenBlockAnalyzerPalettePolicy.colorForDistance(
                0.0, "minecraft:blue_ice", 1, 50);
        int fill = HiddenBlockAnalyzerPalettePolicy.fillColorForDistance(
                0.0, "minecraft:blue_ice", 1, 50);

        assertEquals(0x806FE7F7, outline);
        assertEquals(0x266FE7F7, fill);
    }

    @Test
    void markerGeometryInsetRemainsValid() {
        float filledInset = HiddenBlockAnalyzerPalettePolicy.ANALYZER_BOX_INSET
                + HiddenBlockAnalyzerPalettePolicy.ANALYZER_EDGE_THICKNESS;
        assertTrue(filledInset > 0.0f && filledInset < 0.5f);
    }
}
