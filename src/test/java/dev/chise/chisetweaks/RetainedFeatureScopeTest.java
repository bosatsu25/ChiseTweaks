package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RetainedFeatureScopeTest {
    @Test
    void canonicalScopeContainsExactlyTheElevenRetainedFeatures() {
        assertEquals(List.of(
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.LAVA_HIGHLIGHT), FeatureDefinition.VALUES);
        assertEquals(11, FeatureDefinition.VALUES.size());
        assertTrue(FeatureDefinition.VALUES.stream().allMatch(definition -> definition.area() == FeatureArea.RENDERING));
        assertTrue(FeatureDefinition.VALUES.stream().allMatch(definition -> definition.dependency().isEmpty()));
    }

    @Test
    void qualitySummaryReportsQualityMetricsWithoutDuplicatingFeatureCardinality() throws IOException {
        String summaryScript = Files.readString(Path.of("scripts/quality_summary.py"));

        assertTrue(summaryScript.contains("junit_totals"));
        assertTrue(summaryScript.contains("jacoco_lines"));
        assertTrue(summaryScript.contains("pit_totals"));
        assertTrue(summaryScript.contains("retained-scope quality summary"));
        assertFalse(summaryScript.contains("eleven retained features"));
        assertFalse(summaryScript.contains("ten retained features"));
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
    void modelHighlightsSceneFiltersFireLavaAndAncientDebrisStayOutsideWorksiteModeCoupling() {
        assertFalse(FeatureDefinition.BUILDER_FOCUS_BLOCKS.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.BUILDER_FOCUS_ENTITIES.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.GLASS_INSPECTION.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.MATERIAL_HIGHLIGHTS.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.KELP_HIGHLIGHT.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.FIRE_VISIBILITY.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.LAVA_HIGHLIGHT.isWorksiteVisibilityMode());
        assertEquals(null, FeatureDefinition.BUILDER_FOCUS_BLOCKS.inspectionCategory());
        assertEquals(null, FeatureDefinition.BUILDER_FOCUS_ENTITIES.inspectionCategory());
        assertEquals(null, FeatureDefinition.GLASS_INSPECTION.inspectionCategory());
        assertEquals(null, FeatureDefinition.MATERIAL_HIGHLIGHTS.inspectionCategory());
        assertEquals(null, FeatureDefinition.KELP_HIGHLIGHT.inspectionCategory());
        assertEquals(null, FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.inspectionCategory());
        assertEquals(null, FeatureDefinition.FIRE_VISIBILITY.inspectionCategory());
        assertEquals(null, FeatureDefinition.LAVA_HIGHLIGHT.inspectionCategory());
    }

    @Test
    void persistentGlobalSwitchRegistryContainsEightConfigBackedFeatureSwitches() {
        assertEquals(List.of(
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.KELP_HIGHLIGHT),
                FeatureSwitches.VALUES.stream().map(switchValue -> switchValue.definition()).toList());

        assertEquals(List.of(
                "builderFocusBlocks",
                "builderFocusEntities",
                "fineThreadTrace",
                "hiddenSurfaceTrace",
                "glassInspection",
                "materialHighlights",
                "netherPalette",
                "kelpHighlight"),
                FeatureSwitches.VALUES.stream().map(switchValue -> switchValue.getName()).toList());
    }

    @Test
    void retainedEnglishNamesMatchCurrentProductTerminology() {
        assertEquals("Scene Filter: Blocks", FeatureDefinition.BUILDER_FOCUS_BLOCKS.englishName());
        assertEquals("Scene Filter: Entities", FeatureDefinition.BUILDER_FOCUS_ENTITIES.englishName());
        assertEquals("Fine Thread Trace", FeatureDefinition.FINE_THREAD_TRACE.englishName());
        assertEquals("Hidden Surface Trace", FeatureDefinition.HIDDEN_SURFACE_TRACE.englishName());
        assertEquals("Glass Highlight", FeatureDefinition.GLASS_INSPECTION.englishName());
        assertEquals("Ore Highlights", FeatureDefinition.MATERIAL_HIGHLIGHTS.englishName());
        assertEquals("Nether Palette", FeatureDefinition.NETHER_PALETTE.englishName());
        assertEquals("Kelp Highlight", FeatureDefinition.KELP_HIGHLIGHT.englishName());
        assertEquals("Ancient Debris Analyzer", FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.englishName());
        assertEquals("Low Fire", FeatureDefinition.FIRE_VISIBILITY.englishName());
        assertEquals("Lava Analyzer", FeatureDefinition.LAVA_HIGHLIGHT.englishName());
    }
}
