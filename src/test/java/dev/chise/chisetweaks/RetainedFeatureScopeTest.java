package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RetainedFeatureScopeTest {
    @Test
    void canonicalScopeContainsExactlyTheNineRetainedFeatures() {
        assertEquals(List.of(
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.FIRE_VISIBILITY,
                FeatureDefinition.LAVA_HIGHLIGHT), FeatureDefinition.VALUES);
        assertEquals(9, FeatureDefinition.VALUES.size());
        assertTrue(FeatureDefinition.VALUES.stream().allMatch(definition -> definition.area() == FeatureArea.RENDERING));
        assertTrue(FeatureDefinition.VALUES.stream().allMatch(definition -> definition.dependency().isEmpty()));
    }

    @Test
    void qualitySummaryAlsoReportsNineRetainedFeatures() throws IOException {
        String summaryScript = Files.readString(Path.of("scripts/quality_summary.py"));
        assertTrue(summaryScript.contains("nine retained features"));
        assertFalse(summaryScript.contains("eight retained features"));
    }

    @Test
    void scanBackedFeaturesUseOnlyTheFiveRetainedWorksiteModes() {
        Set<WorksiteVisibilitySelectionPolicy.Mode> modes = FeatureDefinition.VALUES.stream()
                .filter(FeatureDefinition::isWorksiteVisibilityMode)
                .map(FeatureDefinition::worksiteMode)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                WorksiteVisibilitySelectionPolicy.Mode.FINE_THREAD,
                WorksiteVisibilitySelectionPolicy.Mode.HIDDEN_SURFACE,
                WorksiteVisibilitySelectionPolicy.Mode.GLASS,
                WorksiteVisibilitySelectionPolicy.Mode.MATERIAL_HIGHLIGHT,
                WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE), modes);

        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE.inspectionCategory());
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE.inspectionCategory());
        assertEquals(BlockInspectionCategory.GLASS_INSPECTION,
                FeatureDefinition.GLASS_INSPECTION.inspectionCategory());
        assertEquals(BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                FeatureDefinition.MATERIAL_HIGHLIGHTS.inspectionCategory());
        assertEquals(BlockInspectionCategory.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE.inspectionCategory());
    }

    @Test
    void sceneFiltersFireVisibilityAndLavaAnalyzerStayOutsideWorksiteModeCoupling() {
        assertFalse(FeatureDefinition.BUILDER_FOCUS_BLOCKS.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.BUILDER_FOCUS_ENTITIES.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.FIRE_VISIBILITY.isWorksiteVisibilityMode());
        assertFalse(FeatureDefinition.LAVA_HIGHLIGHT.isWorksiteVisibilityMode());
        assertEquals(null, FeatureDefinition.BUILDER_FOCUS_BLOCKS.inspectionCategory());
        assertEquals(null, FeatureDefinition.BUILDER_FOCUS_ENTITIES.inspectionCategory());
        assertEquals(null, FeatureDefinition.FIRE_VISIBILITY.inspectionCategory());
        assertEquals(null, FeatureDefinition.LAVA_HIGHLIGHT.inspectionCategory());
    }

    @Test
    void persistentGlobalSwitchRegistryContainsSevenConfigBackedFeatureSwitches() {
        assertEquals(List.of(
                FeatureDefinition.BUILDER_FOCUS_BLOCKS,
                FeatureDefinition.BUILDER_FOCUS_ENTITIES,
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.NETHER_PALETTE),
                FeatureSwitches.VALUES.stream().map(switchValue -> switchValue.definition()).toList());

        assertEquals(List.of(
                "builderFocusBlocks",
                "builderFocusEntities",
                "fineThreadTrace",
                "hiddenSurfaceTrace",
                "glassInspection",
                "materialHighlights",
                "netherPalette"),
                FeatureSwitches.VALUES.stream().map(switchValue -> switchValue.getName()).toList());
    }

    @Test
    void retainedEnglishNamesMatchCurrentProductTerminology() {
        assertEquals("Scene Filter: Blocks", FeatureDefinition.BUILDER_FOCUS_BLOCKS.englishName());
        assertEquals("Scene Filter: Entities", FeatureDefinition.BUILDER_FOCUS_ENTITIES.englishName());
        assertEquals("Fine Thread Trace", FeatureDefinition.FINE_THREAD_TRACE.englishName());
        assertEquals("Hidden Surface Trace", FeatureDefinition.HIDDEN_SURFACE_TRACE.englishName());
        assertEquals("Glass Inspection", FeatureDefinition.GLASS_INSPECTION.englishName());
        assertEquals("Ore Highlights", FeatureDefinition.MATERIAL_HIGHLIGHTS.englishName());
        assertEquals("Nether Palette", FeatureDefinition.NETHER_PALETTE.englishName());
        assertEquals("Fire Visibility", FeatureDefinition.FIRE_VISIBILITY.englishName());
        assertEquals("Lava Analyzer", FeatureDefinition.LAVA_HIGHLIGHT.englishName());
    }
}
