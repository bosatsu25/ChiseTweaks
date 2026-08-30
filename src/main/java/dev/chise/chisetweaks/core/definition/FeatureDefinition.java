package dev.chise.chisetweaks.core.definition;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;

import java.util.List;
import java.util.Objects;

public enum FeatureDefinition {
    BUILDER_FOCUS_BLOCKS(
            "builder_focus_blocks",
            FeatureArea.RENDERING,
            "config.name.builderfocusblocks",
            "Block Filter",
            "",
            FeatureHelpLevel.ADVANCED,
            null),
    BUILDER_FOCUS_ENTITIES(
            "builder_focus_entities",
            FeatureArea.RENDERING,
            "config.name.builderfocusentities",
            "Entity Filter",
            "",
            FeatureHelpLevel.ADVANCED,
            null),
    FINE_THREAD_TRACE(
            "fine_thread_trace",
            FeatureArea.RENDERING,
            "config.name.finethreadtrace",
            "Fine Line / Tripwire",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            BlockInspectionCategory.TECHNICAL_TRACE),
    HIDDEN_SURFACE_TRACE(
            "hidden_surface_trace",
            FeatureArea.RENDERING,
            "config.name.hiddensurfacetrace",
            "Occluded Blocks",
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
            "Nether Highlight",
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
    FIRE_VISIBILITY(
            "fire_visibility",
            FeatureArea.RENDERING,
            "config.name.localfirevisibility",
            "Low Fire",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    HANDHELD_SIZE(
            "handheld_size",
            FeatureArea.RENDERING,
            "config.name.handheldsize",
            "Handheld Size",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    LAVA_HIGHLIGHT(
            "lava_highlight",
            FeatureArea.RENDERING,
            "config.name.locallavahighlight",
            "Lava Source",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    VILLAGER_ANALYZER(
            "villager_analyzer",
            FeatureArea.RENDERING,
            "config.name.villageranalyzer",
            "Villager Job Site Links",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    BEACON_RANGE(
            "beacon_range",
            FeatureArea.RENDERING,
            "config.name.beaconrange",
            "Beacon Range",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    LIGHTNING_ROD_RANGE(
            "lightning_rod_range",
            FeatureArea.RENDERING,
            "config.name.lightningrodrange",
            "Lightning Rod Range",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    BRIGHT_CHEST(
            "bright_chest",
            FeatureArea.RENDERING,
            "config.name.brightchest",
            "Bright Chest",
            "",
            FeatureHelpLevel.DIAGNOSTIC,
            null),
    BRIGHT_CONCRETE(
            "bright_concrete",
            FeatureArea.RENDERING,
            "config.name.brightconcrete",
            "Bright Concrete",
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
