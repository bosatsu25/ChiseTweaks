package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSwitch;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.gui.help.FeatureHelpCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualFeatureScopeTest {
    private static final Set<String> EXPECTED = Set.of(
            "pumpkin_scaffold", "builder_focus_blocks", "builder_focus_entities", "fine_thread_trace",
            "hidden_surface_trace", "glass_inspection", "placement_guide", "material_highlights",
            "nether_palette", "lava_highlight");

    @Test
    void definitionsAndHelpContainExpectedCapabilities() {
        assertEquals(EXPECTED, FeatureDefinition.VALUES.stream()
                .map(FeatureDefinition::id)
                .collect(Collectors.toSet()));
        assertEquals(EXPECTED, FeatureHelpCatalog.entries().stream()
                .map(entry -> entry.id())
                .collect(Collectors.toSet()));
    }

    @Test
    void buildingHelpContainsOnlyPumpkinScaffold() {
        var buildingEntries = FeatureHelpCatalog.entriesFor(FeatureArea.BUILDING);
        assertEquals(1, buildingEntries.size());
        assertEquals("pumpkin_scaffold", buildingEntries.getFirst().id());
    }

    @Test
    void toggleDefinitionsCoverAllUserFacingCapabilitiesExactlyOnce() {
        java.util.Set<String> mapped = new java.util.HashSet<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            assertTrue(mapped.add(toggle.definition().id()), toggle.getName());
        }
        for (LocalFeatureSwitch toggle : LocalFeatureSwitches.VALUES) {
            assertTrue(mapped.add(toggle.definition().id()), toggle.getName());
        }
        assertEquals(EXPECTED, mapped);
    }

    @Test
    void allFeatureTogglesAreOffByDefault() {
        assertEquals(9, FeatureSwitches.VALUES.size());
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            assertFalse(toggle.getDefaultBooleanValue(), toggle.getName());
        }
        assertEquals(1, LocalFeatureSwitches.VALUES.size());
        for (LocalFeatureSwitch toggle : LocalFeatureSwitches.VALUES) {
            assertFalse(toggle.getDefaultBooleanValue(), toggle.getName());
        }
    }
}
