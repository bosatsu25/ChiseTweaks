package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class WorksiteHighlightProfilePolicyTest {
    private static final VisualAssistanceStylePolicy.OverlayStyle TECH_BASE =
            new VisualAssistanceStylePolicy.OverlayStyle(
                    0xFFB29CFF, 100, VisualAssistanceStylePolicy.Marker.CROSS);
    private static final VisualAssistanceStylePolicy.OverlayStyle HIDDEN_BASE =
            new VisualAssistanceStylePolicy.OverlayStyle(
                    0xFFA68BFF, 90, VisualAssistanceStylePolicy.Marker.CROSS);
    private static final VisualAssistanceStylePolicy.OverlayStyle MATERIAL_BASE =
            new VisualAssistanceStylePolicy.OverlayStyle(
                    0xFF123456, 80, VisualAssistanceStylePolicy.Marker.DIAGONAL);

    @Test
    void colorAndOpacityInputsAreBoundedAndHumanReadable() {
        assertEquals(-1, WorksiteHighlightProfilePolicy.clampColorPreset(Integer.MIN_VALUE));
        assertEquals(-1, WorksiteHighlightProfilePolicy.clampColorPreset(-1));
        assertEquals(3, WorksiteHighlightProfilePolicy.clampColorPreset(3));
        assertEquals(7, WorksiteHighlightProfilePolicy.clampColorPreset(Integer.MAX_VALUE));
        assertEquals("AUTO", WorksiteHighlightProfilePolicy.colorLabel(-99));
        assertEquals("#B29CFF", WorksiteHighlightProfilePolicy.colorLabel(0));
        assertEquals("#FF6B6B", WorksiteHighlightProfilePolicy.colorLabel(99));

        assertEquals(20, WorksiteHighlightProfilePolicy.clampOpacityPercent(Integer.MIN_VALUE));
        assertEquals(65, WorksiteHighlightProfilePolicy.clampOpacityPercent(65));
        assertEquals(100, WorksiteHighlightProfilePolicy.clampOpacityPercent(Integer.MAX_VALUE));
    }

    @Test
    void overworldAndManualProfilesKeepRequestedBoundedDistance() {
        assertEquals(
                new WorksiteHighlightProfilePolicy.ScanProfile(5, 3),
                WorksiteHighlightProfilePolicy.scanProfile(
                        5, 3, false, WorksiteHighlightProfilePolicy.DimensionProfile.OVERWORLD));
        assertEquals(
                new WorksiteHighlightProfilePolicy.ScanProfile(8, 1),
                WorksiteHighlightProfilePolicy.scanProfile(
                        100, -100, false, WorksiteHighlightProfilePolicy.DimensionProfile.NETHER));
        assertEquals(
                new WorksiteHighlightProfilePolicy.ScanProfile(5, 3),
                WorksiteHighlightProfilePolicy.scanProfile(5, 3, true, null));
    }

    @Test
    void netherPresetReducesHorizontalWorkWithoutBreakingMinimumRadius() {
        assertEquals(
                new WorksiteHighlightProfilePolicy.ScanProfile(4, 3),
                WorksiteHighlightProfilePolicy.scanProfile(
                        5, 3, true, WorksiteHighlightProfilePolicy.DimensionProfile.NETHER));
        assertEquals(
                new WorksiteHighlightProfilePolicy.ScanProfile(1, 1),
                WorksiteHighlightProfilePolicy.scanProfile(
                        1, 1, true, WorksiteHighlightProfilePolicy.DimensionProfile.NETHER));
    }

    @Test
    void defaultOverworldStylePreservesExistingTracePalette() {
        assertEquals(
                TECH_BASE,
                WorksiteHighlightProfilePolicy.customize(
                        TECH_BASE,
                        BlockInspectionCategory.TECHNICAL_TRACE,
                        -1, 100,
                        -1, 100,
                        false,
                        WorksiteHighlightProfilePolicy.DimensionProfile.OVERWORLD));
        assertEquals(
                HIDDEN_BASE,
                WorksiteHighlightProfilePolicy.customize(
                        HIDDEN_BASE,
                        BlockInspectionCategory.HIDDEN_SURFACE,
                        -1, 100,
                        -1, 100,
                        false,
                        WorksiteHighlightProfilePolicy.DimensionProfile.OVERWORLD));
    }

    @Test
    void manualColorAndOpacityOverrideOnlyTraceCategories() {
        var technical = WorksiteHighlightProfilePolicy.customize(
                TECH_BASE,
                BlockInspectionCategory.TECHNICAL_TRACE,
                6, 60,
                -1, 100,
                false,
                WorksiteHighlightProfilePolicy.DimensionProfile.OVERWORLD);
        assertEquals(0x99FF9F5A, technical.argb());
        assertEquals(TECH_BASE.priority(), technical.priority());
        assertEquals(TECH_BASE.marker(), technical.marker());

        var hidden = WorksiteHighlightProfilePolicy.customize(
                HIDDEN_BASE,
                BlockInspectionCategory.HIDDEN_SURFACE,
                -1, 100,
                4, 20,
                false,
                WorksiteHighlightProfilePolicy.DimensionProfile.OVERWORLD);
        assertEquals(0x33FF72D2, hidden.argb());

        assertSame(MATERIAL_BASE, WorksiteHighlightProfilePolicy.customize(
                MATERIAL_BASE,
                BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                6, 20,
                4, 20,
                true,
                WorksiteHighlightProfilePolicy.DimensionProfile.NETHER));
    }

    @Test
    void netherAutoPresetUsesHighContrastDefaultsAndOpacityBoost() {
        var technical = WorksiteHighlightProfilePolicy.customize(
                TECH_BASE,
                BlockInspectionCategory.TECHNICAL_TRACE,
                -1, 70,
                -1, 70,
                true,
                WorksiteHighlightProfilePolicy.DimensionProfile.NETHER);
        assertEquals(0xCC6FE7F7, technical.argb());

        var hidden = WorksiteHighlightProfilePolicy.customize(
                HIDDEN_BASE,
                BlockInspectionCategory.HIDDEN_SURFACE,
                -1, 95,
                -1, 95,
                true,
                WorksiteHighlightProfilePolicy.DimensionProfile.NETHER);
        assertEquals(0xFFFFD166, hidden.argb());

        var explicit = WorksiteHighlightProfilePolicy.customize(
                TECH_BASE,
                BlockInspectionCategory.TECHNICAL_TRACE,
                7, 95,
                -1, 100,
                true,
                WorksiteHighlightProfilePolicy.DimensionProfile.NETHER);
        assertEquals(0xFFFF6B6B, explicit.argb());
    }

    @Test
    void invalidOrInvisibleInputFailsClosed() {
        assertEquals(
                VisualAssistanceStylePolicy.OverlayStyle.NONE,
                WorksiteHighlightProfilePolicy.customize(
                        null,
                        BlockInspectionCategory.TECHNICAL_TRACE,
                        -1, 100, -1, 100, false,
                        WorksiteHighlightProfilePolicy.DimensionProfile.OTHER));
        assertEquals(
                VisualAssistanceStylePolicy.OverlayStyle.NONE,
                WorksiteHighlightProfilePolicy.customize(
                        VisualAssistanceStylePolicy.OverlayStyle.NONE,
                        BlockInspectionCategory.TECHNICAL_TRACE,
                        -1, 100, -1, 100, false,
                        WorksiteHighlightProfilePolicy.DimensionProfile.OTHER));
        assertEquals(
                VisualAssistanceStylePolicy.OverlayStyle.NONE,
                WorksiteHighlightProfilePolicy.customize(
                        TECH_BASE,
                        null,
                        -1, 100, -1, 100, false,
                        WorksiteHighlightProfilePolicy.DimensionProfile.OTHER));
    }
}
