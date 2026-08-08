package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.core.policy.ModVersionPolicy;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QualityGatePolicyCoverageTest {
    @Test
    void builderEntityVisibilityIsFailClosedAndKeepsTheLocalPlayerVisible() {
        var blacklist = Set.of("minecraft:zombie", "minecraft:player");
        var whitelist = Set.of("minecraft:armor_stand", "minecraft:player");

        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW_DISABLED,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        false, false, "minecraft:zombie", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                        blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW_LOCAL_PLAYER,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, true, "minecraft:player", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                        blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, false, null, BuilderEntityVisibilityPolicy.Mode.NONE, blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.HIDE,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, false, "minecraft:zombie", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                        blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, false, "minecraft:armor_stand", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                        blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, false, "minecraft:armor_stand", BuilderEntityVisibilityPolicy.Mode.WHITELIST,
                        blacklist, whitelist)));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.HIDE,
                BuilderEntityVisibilityPolicy.evaluate(new BuilderEntityVisibilityPolicy.Input(
                        true, false, "minecraft:zombie", BuilderEntityVisibilityPolicy.Mode.WHITELIST,
                        blacklist, whitelist)));

        assertTrue(BuilderEntityVisibilityPolicy.filtersPlayers(
                BuilderEntityVisibilityPolicy.Mode.BLACKLIST, blacklist, whitelist));
        assertFalse(BuilderEntityVisibilityPolicy.filtersPlayers(
                BuilderEntityVisibilityPolicy.Mode.WHITELIST, blacklist, whitelist));
        assertThrows(NullPointerException.class, () -> BuilderEntityVisibilityPolicy.evaluate(null));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:zombie", null, blacklist, whitelist));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:zombie", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                null, whitelist));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:zombie", BuilderEntityVisibilityPolicy.Mode.BLACKLIST,
                blacklist, null));
    }

    @Test
    void lavaPaletteOnlyTintsTheRequestedLavaKind() {
        int source = 0x00112233;
        int flowing = 0x00445566;

        assertEquals(LavaVisionPalettePolicy.NO_TINT,
                LavaVisionPalettePolicy.color(false, true, true, true, source, flowing));
        assertEquals(0xFF112233,
                LavaVisionPalettePolicy.color(true, true, true, false, source, flowing));
        assertEquals(0xFF445566,
                LavaVisionPalettePolicy.color(true, false, false, true, source, flowing));
        assertEquals(LavaVisionPalettePolicy.NO_TINT,
                LavaVisionPalettePolicy.color(true, true, false, true, source, flowing));
        assertEquals(LavaVisionPalettePolicy.NO_TINT,
                LavaVisionPalettePolicy.color(true, false, true, false, source, flowing));
    }

    @Test
    void versionPolicyComparesNumericReleasesAndRejectsMalformedInput() {
        assertTrue(ModVersionPolicy.meetsMinimum("v1.2.3+mc26.1.2", "1.2.3"));
        assertTrue(ModVersionPolicy.meetsMinimum("1.2.3.0", "1.2.3"));
        assertTrue(ModVersionPolicy.meetsMinimum("1.3.0", "1.2.99"));
        assertFalse(ModVersionPolicy.meetsMinimum("1.2.2", "1.2.3"));
        assertFalse(ModVersionPolicy.meetsMinimum(null, "1.0.0"));
        assertFalse(ModVersionPolicy.meetsMinimum("1.0.0", "1..0"));

        assertTrue(ModVersionPolicy.matchesPinnedRelease("V2.4.0-beta.1", "2.4"));
        assertFalse(ModVersionPolicy.matchesPinnedRelease("2.4.1", "2.4"));
        assertFalse(ModVersionPolicy.matchesPinnedRelease("invalid", "2.4"));

        assertEquals(0, ModVersionPolicy.compareRelease(null, "bad"));
        assertTrue(ModVersionPolicy.compareRelease("bad", "1.0") < 0);
        assertTrue(ModVersionPolicy.compareRelease("1.0", "bad") > 0);
        assertEquals(0, ModVersionPolicy.compareRelease("1.2", "1.2.0"));
        assertTrue(ModVersionPolicy.compareRelease("1.10", "1.2") > 0);
        assertTrue(ModVersionPolicy.compareRelease("1.2", "1.10") < 0);
        assertTrue(ModVersionPolicy.compareRelease("2.0-rc.1", "1.99+build") > 0);

        assertFalse(ModVersionPolicy.meetsMinimum("", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("1.", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("1.a", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("21474836470", "1"));
    }

    @Test
    void orientationPolicyNormalizesStateAndIgnoresInvalidTokens() {
        assertEquals(OrientationOverlayPolicy.Overlay.EMPTY, OrientationOverlayPolicy.inspect(null));
        assertTrue(OrientationOverlayPolicy.inspect(Map.of()).empty());

        var overlay = OrientationOverlayPolicy.inspect(Map.of(
                "facing", " East ",
                "axis", "z",
                "half", "top",
                "shape", "inner_left",
                "face", "wall",
                "open", "TRUE"));
        assertEquals(OrientationOverlayPolicy.Facing.EAST, overlay.facing());
        assertEquals(OrientationOverlayPolicy.Axis.Z, overlay.axis());
        assertEquals(OrientationOverlayPolicy.Half.TOP, overlay.half());
        assertEquals(OrientationOverlayPolicy.Shape.INNER_LEFT, overlay.shape());
        assertEquals(OrientationOverlayPolicy.MountFace.WALL, overlay.mountFace());
        assertEquals(Boolean.TRUE, overlay.open());
        assertFalse(overlay.empty());

        var slab = OrientationOverlayPolicy.inspect(Map.of("type", "bottom", "open", "false"));
        assertEquals(OrientationOverlayPolicy.Half.BOTTOM, slab.half());
        assertEquals(Boolean.FALSE, slab.open());

        assertTrue(OrientationOverlayPolicy.parseFacing("north").isPresent());
        assertTrue(OrientationOverlayPolicy.parseAxis("x").isPresent());
        assertTrue(OrientationOverlayPolicy.parseHalf("upper").isPresent());
        assertTrue(OrientationOverlayPolicy.parseShape("outer_right").isPresent());
        assertTrue(OrientationOverlayPolicy.parseMountFace("ceiling").isPresent());
        assertTrue(OrientationOverlayPolicy.parseBoolean(" false ").isPresent());
        assertTrue(OrientationOverlayPolicy.parseFacing(null).isEmpty());
        assertTrue(OrientationOverlayPolicy.parseFacing("sideways").isEmpty());
        assertTrue(OrientationOverlayPolicy.parseBoolean(null).isEmpty());
        assertTrue(OrientationOverlayPolicy.parseBoolean("yes").isEmpty());
        assertTrue(OrientationOverlayPolicy.inspect(Map.of("type", "double")).empty());
    }

    @Test
    void worksiteSelectionAndBudgetsRemainBounded() {
        var glass = WorksiteVisibilitySelectionPolicy.Mode.GLASS;
        var material = WorksiteVisibilitySelectionPolicy.Mode.MATERIAL_HIGHLIGHT;

        assertEquals(Set.of(glass), WorksiteVisibilitySelectionPolicy.afterToggle(null, glass, true, false));
        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.afterToggle(Set.of(glass), glass, false, false));
        assertEquals(Set.of(material), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(glass), material, true, true));
        assertEquals(Set.of(glass, material), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(glass), material, true, false));
        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.normalize(null, true));
        assertEquals(Set.of(glass), WorksiteVisibilitySelectionPolicy.normalize(Set.of(glass), true));
        assertEquals(Set.of(glass, material), WorksiteVisibilitySelectionPolicy.normalize(
                Set.of(glass, material), false));
        assertEquals(1, WorksiteVisibilitySelectionPolicy.normalize(Set.of(glass, material), true).size());

        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MIN_VALUE));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(0));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(6));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(0));
        assertEquals(100, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(101));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampResults(0));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHudResults(99));
        assertEquals(24, WorksiteVisibilityBudgetPolicy.clampOverlayResults(99));
        assertEquals(27, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(1, 1));
        assertEquals(3179, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(99, 99));
    }

    @Test
    void visualStylePolicyUsesStablePriorityMarkerAndArgbMappings() {
        assertEquals(VisualAssistanceStylePolicy.OverlayStyle.NONE,
                VisualAssistanceStylePolicy.styleFor("minecraft:stone", null));
        assertEquals(VisualAssistanceStylePolicy.OverlayStyle.NONE,
                VisualAssistanceStylePolicy.styleFor("minecraft:stone", BlockInspectionCategory.NONE));
        assertFalse(VisualAssistanceStylePolicy.OverlayStyle.NONE.visible());

        var technical = VisualAssistanceStylePolicy.styleFor(null, BlockInspectionCategory.TECHNICAL_TRACE);
        assertEquals(0xFFFFC857, technical.argb());
        assertEquals(100, technical.priority());
        assertEquals(VisualAssistanceStylePolicy.Marker.CROSS, technical.marker());
        assertTrue(technical.visible());

        assertEquals(0xFFBDEBFF, VisualAssistanceStylePolicy.styleFor(
                "minecraft:powder_snow", BlockInspectionCategory.HIDDEN_SURFACE).argb());
        assertEquals(0xFF6DB7FF, VisualAssistanceStylePolicy.styleFor(
                "minecraft:blue_ice", BlockInspectionCategory.HIDDEN_SURFACE).argb());
        assertEquals(0xFFB8A58A, VisualAssistanceStylePolicy.styleFor(
                "minecraft:dead_brain_coral", BlockInspectionCategory.HIDDEN_SURFACE).argb());
        assertEquals(0xFF6EE7B7, VisualAssistanceStylePolicy.styleFor(
                "minecraft:stone", BlockInspectionCategory.HIDDEN_SURFACE).argb());

        assertEquals(VisualAssistanceStylePolicy.Marker.BOX, VisualAssistanceStylePolicy.styleFor(
                "minecraft:glass", BlockInspectionCategory.GLASS_INSPECTION).marker());
        assertEquals(VisualAssistanceStylePolicy.Marker.ORIENTATION, VisualAssistanceStylePolicy.styleFor(
                "minecraft:oak_stairs", BlockInspectionCategory.PLACEMENT_GUIDE).marker());

        assertEquals(0xFF50E3E6, material("DIAMOND"));
        assertEquals(0xFF57E389, material("emerald"));
        assertEquals(0xFFFF5A5A, material("redstone"));
        assertEquals(0xFF5A7DFF, material("lapis"));
        assertEquals(0xFFFFD24A, material("gold"));
        assertEquals(0xFFFF9B62, material("copper"));
        assertEquals(0xFFE4D8C8, material("iron"));
        assertEquals(0xFFA6A6A6, material("coal"));
        assertEquals(0xFFFF8B6B, material("ancient_debris"));
        assertEquals(0xFFC6A0FF, material("obsidian"));

        assertEquals(0xFF52D7D0, nether("warped_stem"));
        assertEquals(0xFFE45A72, nether("crimson_stem"));
        assertEquals(0xFFE45A72, nether("nether_wart_block"));
        assertEquals(0xFF69C9E8, nether("soul_soil"));
        assertEquals(0xFFB49ACF, nether("blackstone"));
        assertEquals(0xFFB7B7B7, nether("basalt"));
        assertEquals(0xFFFFD166, nether("glowstone"));
        assertEquals(0xFFFFD166, nether("shroomlight"));
        assertEquals(0xFFE38C78, nether("netherrack"));
    }

    private static int material(String id) {
        return VisualAssistanceStylePolicy.styleFor(
                "minecraft:" + id, BlockInspectionCategory.MATERIAL_HIGHLIGHT).argb();
    }

    private static int nether(String id) {
        return VisualAssistanceStylePolicy.styleFor(
                "minecraft:" + id, BlockInspectionCategory.NETHER_PALETTE).argb();
    }
}
