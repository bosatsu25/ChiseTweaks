package dev.chise.chisetweaks.gui.help;

import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureHelpLevel;

import java.util.Objects;

/** One user-facing help card in the in-game Chise navigation screen. */
public record FeatureHelpEntry(
        String id,
        FeatureArea area,
        String nameKey,
        String englishName,
        String dependency,
        FeatureHelpLevel level,
        boolean defaultOff) {

    public FeatureHelpEntry {
        id = requireText(id, "id");
        area = Objects.requireNonNull(area, "area");
        nameKey = requireText(nameKey, "nameKey");
        englishName = requireText(englishName, "englishName");
        dependency = dependency == null ? "" : dependency.trim();
        level = Objects.requireNonNull(level, "level");
    }

    public String summaryKey() { return "help.chisetweaks." + id + ".summary"; }
    public String usageKey() { return "help.chisetweaks." + id + ".usage"; }
    public String requirementKey() { return "help.chisetweaks." + id + ".requirement"; }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
