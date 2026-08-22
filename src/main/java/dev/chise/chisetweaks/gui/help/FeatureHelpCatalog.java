package dev.chise.chisetweaks.gui.help;

import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;

import java.util.List;

public final class FeatureHelpCatalog {
    private static final List<FeatureHelpEntry> ENTRIES = FeatureDefinition.VALUES.stream()
            .map(definition -> new FeatureHelpEntry(
                    definition.id(),
                    definition.area(),
                    definition.nameKey(),
                    definition.englishName(),
                    definition.dependency(),
                    definition.helpLevel(),
                    true))
            .toList();

    private FeatureHelpCatalog() {}

    public static List<FeatureHelpEntry> entries() { return ENTRIES; }

    public static List<FeatureHelpEntry> entriesFor(FeatureArea area) {
        if (area == null) return ENTRIES;
        return ENTRIES.stream().filter(entry -> entry.area() == area).toList();
    }
}
