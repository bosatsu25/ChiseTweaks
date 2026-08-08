package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSwitch;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.gui.help.FeatureHelpCatalog;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureDefinitionConsistencyTest {
    @Test
    void helpCatalogUsesExactlyTheDefinitionIds() {
        Set<String> expected = FeatureDefinition.VALUES.stream()
                .map(FeatureDefinition::id)
                .collect(Collectors.toSet());

        assertEquals(expected, FeatureHelpCatalog.entries().stream()
                .map(entry -> entry.id())
                .collect(Collectors.toSet()));
    }

    @Test
    void worksiteDefinitionsCoverEveryModeAndInspectionCategoryExactlyOnce() {
        var definitions = FeatureDefinition.VALUES.stream()
                .filter(FeatureDefinition::isWorksiteVisibilityMode)
                .toList();

        assertEquals(6, definitions.size());
        assertEquals(
                EnumSet.allOf(WorksiteVisibilitySelectionPolicy.Mode.class),
                definitions.stream()
                        .map(FeatureDefinition::worksiteMode)
                        .collect(Collectors.toCollection(() ->
                                EnumSet.noneOf(WorksiteVisibilitySelectionPolicy.Mode.class))));
        assertEquals(
                Set.of(
                        BlockInspectionCategory.TECHNICAL_TRACE,
                        BlockInspectionCategory.HIDDEN_SURFACE,
                        BlockInspectionCategory.GLASS_INSPECTION,
                        BlockInspectionCategory.PLACEMENT_GUIDE,
                        BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                        BlockInspectionCategory.NETHER_PALETTE),
                definitions.stream()
                        .map(FeatureDefinition::inspectionCategory)
                        .collect(Collectors.toSet()));
    }

    @Test
    void everyDefinitionHasExactlyOneUserFacingToggleOwner() {
        Set<String> owned = new java.util.HashSet<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            assertTrue(owned.add(toggle.definition().id()), toggle.definition().id());
        }
        for (LocalFeatureSwitch toggle : LocalFeatureSwitches.VALUES) {
            assertTrue(owned.add(toggle.definition().id()), toggle.definition().id());
        }
        assertEquals(
                FeatureDefinition.VALUES.stream()
                        .map(FeatureDefinition::id)
                        .collect(Collectors.toSet()),
                owned);
    }
}
