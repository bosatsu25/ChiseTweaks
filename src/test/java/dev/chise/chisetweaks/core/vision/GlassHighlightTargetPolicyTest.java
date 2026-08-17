package dev.chise.chisetweaks.core.vision;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlassHighlightTargetPolicyTest {
    private static final Set<String> COLORS = Set.of(
            "white", "orange", "magenta", "light_blue",
            "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black");

    @Test
    void catalogContainsAllVanillaGlassAndPaneVariants() {
        assertEquals(18, GlassHighlightTargetPolicy.blockVariantCount());
        assertEquals(17, GlassHighlightTargetPolicy.paneVariantCount());
        assertEquals(35, GlassHighlightTargetPolicy.totalVariantCount());
        assertTrue(GlassHighlightTargetPolicy.blockPaths().contains("glass"));
        assertTrue(GlassHighlightTargetPolicy.blockPaths().contains("tinted_glass"));
        assertTrue(GlassHighlightTargetPolicy.panePaths().contains("glass_pane"));

        for (String color : COLORS) {
            assertTrue(GlassHighlightTargetPolicy.blockPaths().contains(color + "_stained_glass"), color);
            assertTrue(GlassHighlightTargetPolicy.panePaths().contains(color + "_stained_glass_pane"), color);
        }
    }

    @Test
    void classifierDistinguishesFullBlocksFromPanesWithoutBroadSuffixMatching() {
        assertEquals(GlassHighlightTargetPolicy.Shape.BLOCK,
                GlassHighlightTargetPolicy.classify("minecraft", "glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.BLOCK,
                GlassHighlightTargetPolicy.classify("minecraft", "tinted_glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.BLOCK,
                GlassHighlightTargetPolicy.classify("minecraft", "red_stained_glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.PANE,
                GlassHighlightTargetPolicy.classify("minecraft", "glass_pane"));
        assertEquals(GlassHighlightTargetPolicy.Shape.PANE,
                GlassHighlightTargetPolicy.classify("minecraft", "red_stained_glass_pane"));

        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classify("minecraft", "glass_bottle"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classify("example", "glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classify(null, "glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classify("minecraft", null));
    }

    @Test
    void blockIdClassifierNormalizesCaseAndRejectsMalformedIds() {
        assertEquals(GlassHighlightTargetPolicy.Shape.BLOCK,
                GlassHighlightTargetPolicy.classifyBlockId("  MINECRAFT:LIGHT_BLUE_STAINED_GLASS  "));
        assertEquals(GlassHighlightTargetPolicy.Shape.PANE,
                GlassHighlightTargetPolicy.classifyBlockId("minecraft:light_blue_stained_glass_pane"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId(null));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId("glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId("minecraft:"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId(":glass"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId("minecraft:glass:extra"));
        assertEquals(GlassHighlightTargetPolicy.Shape.NONE,
                GlassHighlightTargetPolicy.classifyBlockId("minecraft:stone"));
    }

    @Test
    void blockAndPaneCatalogsAreDisjoint() {
        assertTrue(GlassHighlightTargetPolicy.blockPaths().stream()
                .noneMatch(GlassHighlightTargetPolicy.panePaths()::contains));
        assertFalse(GlassHighlightTargetPolicy.blockPaths().isEmpty());
        assertFalse(GlassHighlightTargetPolicy.panePaths().isEmpty());
    }
}
