package dev.chise.chisetweaks.core.definition;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;

import java.util.List;
import java.util.Objects;

public enum FeatureDefinition {
    BUILDER_FOCUS_BLOCKS(
            "builder_focus_blocks",
            FeatureArea.RENDERING,
            "config.name.builderfocusblocks",
            "Scene Filter: Blocks",
            "",
            FeatureHelpLevel.ADVANCED,
            null),
    BUILDER_FOCUS_ENTITIES(
            "builder_focus_entities",
            FeatureArea.RENDERING,
            "config.name.builderfocusentities",
            "Scene Filter: Entities",
            "",
            FeatureHelpLevel.ADVANCED,
            null),
    FINE_THREAD_TRACE(
            "fine_thread_trace",
            FeatureArea.RENDERING,
            "config.name.finethreadtrace",
            "Fine Thread Trace",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            BlockInspectionCategory.TECHNICAL_TRACE),
    HIDDEN_SURFACE_TRACE(
            "hidden_surface_trace",
            FeatureArea.RENDERING,
            "config.name.hiddensurfacetrace",
            "Hidden Surface Trace",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            BlockInspectionCategory.HIDDEN_SURFACE),
    GLASS_INSPECTION(
            "glass_inspection",
            FeatureArea.RENDERING,
            "config.name.glassinspection",
            "Glass Highlight",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    MATERIAL_HIGHLIGHTS(
            "material_highlights",
            FeatureArea.RENDERING,
            "config.name.materialhighlights",
            "Ore Highlights",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    NETHER_PALETTE(
            "nether_palette",
            FeatureArea.RENDERING,
            "config.name.netherpalette",
            "Nether Palette",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            BlockInspectionCategory.NETHER_PALETTE),
    KELP_HIGHLIGHT(
            "kelp_highlight",
            FeatureArea.RENDERING,
            "config.name.kelphighlight",
            "Kelp Highlight",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    ANCIENT_DEBRIS_ANALYZER(
            "ancient_debris_analyzer",
            FeatureArea.RENDERING,
            "config.name.localancientdebrisanalyzer",
            "Ancient Debris Analyzer",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    FIRE_VISIBILITY(
            "fire_visibility",
            FeatureArea.RENDERING,
            "config.name.localfirevisibility",
            "Fire Visibility",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    LAVA_HIGHLIGHT(
            "lava_highlight",
            FeatureArea.RENDERING,
            "config.name.locallavahighlight",
            "Lava Source Highlight",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null);

    public static final List<FeatureDefinition> VALUES = List.of(values());

    private final String id;
    private final FeatureArea area;
    private final String nameKey;
    private final String englishName;
    private final String dependency;
    private final FeatureHelpLevel helpLevel;
    private final BlockInspectionCategory inspectionCategory;

    FeatureDefinition(
            String id,
            FeatureArea area,
            String nameKey,
            String englishName,
            String dependency,
            FeatureHelpLevel helpLevel,
            BlockInspectionCategory inspectionCategory) {
        this.id = requireText(id, "id");
        this.area = Objects.requireNonNull(area, "area");
        this.nameKey = requireText(nameKey, "nameKey");
        this.englishName = requireText(englishName, "englishName");
        this.dependency = dependency == null ? "" : dependency.trim();
        this.helpLevel = Objects.requireNonNull(helpLevel, "helpLevel");
        this.inspectionCategory = inspectionCategory;
    }

    public String id() { return id; }
    public FeatureArea area() { return area; }
    public String nameKey() { return nameKey; }
    public String englishName() { return englishName; }
    public String dependency() { return dependency; }
    public FeatureHelpLevel helpLevel() { return helpLevel; }
    public boolean isWorksiteVisibilityMode() { return inspectionCategory != null; }
    public BlockInspectionCategory inspectionCategory() { return inspectionCategory; }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
