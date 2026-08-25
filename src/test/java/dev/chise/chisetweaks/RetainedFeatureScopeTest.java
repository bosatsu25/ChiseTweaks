package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RetainedFeatureScopeTest {
    @Test
    void canonicalScopeContainsExactlyTheFifteenRetainedFeatures() {
        assertEquals(List.of(
                FeatureDefinition.AIR_PLACEMENT,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER,
                FeatureDefinition.WARDEN_RISK_ANALYZER,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.BRIGHT_CHEST,
                FeatureDefinition.BRIGHT_CONCRETE), FeatureDefinition.VALUES);
        assertEquals(15, FeatureDefinition.VALUES.size());
        assertEquals(FeatureArea.BUILDING, FeatureDefinition.AIR_PLACEMENT.area());
        assertEquals(14, FeatureDefinition.VALUES.stream()
                .filter(definition -> definition.area() == FeatureArea.RENDERING)
                .count());
        assertTrue(FeatureDefinition.VALUES.stream().allMatch(definition -> definition.dependency().isEmpty()));
    }

    @Test
    void scanBackedFeaturesMapDirectlyToTheThreeInspectionCategories() {
        Set<BlockInspectionCategory> categories = Set.copyOf(FeatureDefinition.VALUES.stream()
                .filter(FeatureDefinition::isWorksiteVisibilityMode)
                .map(FeatureDefinition::inspectionCategory)
                .toList());
        assertEquals(Set.of(
                BlockInspectionCategory.TECHNICAL_TRACE,
                BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionCategory.NETHER_PALETTE), categories);

        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE.inspectionCategory());
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE.inspectionCategory());
        assertEquals(BlockInspectionCategory.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE.inspectionCategory());
    }

    @Test
    void nonWorksiteFeaturesStayOutsideWorksiteModeCoupling() {
        for (FeatureDefinition definition : List.of(
                FeatureDefinition.AIR_PLACEMENT,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER,
                FeatureDefinition.WARDEN_RISK_ANALYZER,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.BRIGHT_CHEST,
                FeatureDefinition.BRIGHT_CONCRETE)) {
            assertFalse(definition.isWorksiteVisibilityMode());
            assertEquals(null, definition.inspectionCategory());
        }
    }

    @Test
    void unifiedSwitchRegistryContainsAllFifteenFeatureSwitches() {
        assertEquals(List.of(
                FeatureDefinition.AIR_PLACEMENT,
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER,
                FeatureDefinition.WARDEN_RISK_ANALYZER,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.BRIGHT_CHEST,
                FeatureDefinition.BRIGHT_CONCRETE),
                FeatureSwitches.VALUES.stream().map(value -> value.definition()).toList());
        assertEquals(9, FeatureSwitches.FEATURE_CONFIG_VALUES.size());
        assertEquals(6, FeatureSwitches.LOCAL_CONFIG_VALUES.size());
    }

    @Test
    void retainedEnglishNamesMatchReadmeProductTerminology() {
        assertEquals("Air Placement", FeatureDefinition.AIR_PLACEMENT.englishName());
        assertEquals("Block Filter", FeatureDefinition.BUILDER_FOCUS_BLOCKS.englishName());
        assertEquals("Entity Filter", FeatureDefinition.BUILDER_FOCUS_ENTITIES.englishName());
        assertEquals("Fine Line Highlight", FeatureDefinition.FINE_THREAD_TRACE.englishName());
        assertEquals("Hidden Block Highlight", FeatureDefinition.HIDDEN_SURFACE_TRACE.englishName());
        assertEquals("Glass Highlight", FeatureDefinition.GLASS_INSPECTION.englishName());
        assertEquals("Ore Highlights", FeatureDefinition.MATERIAL_HIGHLIGHTS.englishName());
        assertEquals("Nether Highlight", FeatureDefinition.NETHER_PALETTE.englishName());
        assertEquals("Kelp Highlight", FeatureDefinition.KELP_HIGHLIGHT.englishName());
        assertEquals("Ancient Debris Analyzer", FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.englishName());
        assertEquals("Warden Risk Analyzer", FeatureDefinition.WARDEN_RISK_ANALYZER.englishName());
        assertEquals("Low Fire", FeatureDefinition.FIRE_VISIBILITY.englishName());
        assertEquals("Lava Analyzer", FeatureDefinition.LAVA_HIGHLIGHT.englishName());
        assertEquals("Bright Chest", FeatureDefinition.BRIGHT_CHEST.englishName());
        assertEquals("Bright Concrete", FeatureDefinition.BRIGHT_CONCRETE.englishName());
    }
}
