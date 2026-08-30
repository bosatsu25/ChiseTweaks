package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteHighlightProfilePolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public final class LocalFeatureConfig {
    private static final LocalFeatureConfig INSTANCE = new LocalFeatureConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "chisetweaks-visual.json";
    private static final String LEGACY_PACK_ID = "chisetweaks:chise_texture";
    private static final String LEGACY_CHEST_PACK_ID = "chisetweaks:chise_chest_visibility";
    private static final String LEGACY_CONCRETE_PACK_ID = "chisetweaks:chise_white_concrete_visibility";

    public boolean lavaHighlightEnabled = false;
    public boolean villagerAnalyzerEnabled = false;
    public boolean beaconRangeEnabled = false;
    public boolean lightningRodRangeEnabled = false;
    public boolean fireVisibilityEnabled = false;
    public int fireVisibilitySizePreset = FireVisibilityPolicy.DEFAULT_SIZE_PRESET;
    public boolean brightChestEnabled = true;
    public boolean brightConcreteEnabled = true;
    public boolean oreHighlightAnimationEnabled = false;
    public boolean interactionHistoryEnabled = false;
    public boolean schematicPlacementInspectorEnabled = false;

    public int worksiteVisibilityHorizontalRadius = 5;
    public int worksiteVisibilityVerticalRadius = 3;
    public int worksiteVisibilityIntervalTicks = 10;
    public int worksiteVisibilityMaxOverlayResults = 12;
    public boolean worksiteVisibilityWorldOverlay = true;
    public boolean worksiteVisibilityDimensionPresetsEnabled = false;
    public int fineThreadTraceColorPreset = WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET;
    public int fineThreadTraceOpacityPercent = WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT;
    public int hiddenSurfaceTraceColorPreset = WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET;
    public int hiddenSurfaceTraceOpacityPercent = WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT;

    public int lavaAnalyzerHorizontalRadius = 5;
    public int lavaAnalyzerVerticalRadius = 3;
    public int lavaAnalyzerIntervalTicks = 10;
    public int lavaAnalyzerMaxOverlayResults = 12;

    public int visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
    public int visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;

    LocalFeatureConfig() {}

    public static LocalFeatureConfig getInstance() { return INSTANCE; }

    public synchronized void load() {
        resetToDefaults();
        try {
            Optional<String> stored = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME);
            if (stored.isEmpty() || stored.get().isBlank()) {
                save();
                return;
            }
            String document = stored.get();
            boolean migrateBrightState = needsLegacyBrightMigration(document);
            if (!replaceFromJsonDocument(document)) {
                ChiseTweaksClient.LOGGER.warn("Rejected local config; using safe defaults");
                return;
            }
            if (migrateBrightState && migrateLegacyBrightState()) save();
        } catch (java.io.IOException | RuntimeException error) {
            resetToDefaults();
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to load local config after {}; using safe defaults",
                    error.getClass().getSimpleName());
        }
    }

    boolean replaceFromJsonDocument(String json) {
        resetToDefaults();
        if (json == null || json.isBlank()) return false;

        StrictJsonSecurityPolicy.Validation validation =
                StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) return false;

        try {
            JsonObject source = JsonParser.parseString(json).getAsJsonObject();
            JsonObject defaults = GSON.toJsonTree(new LocalFeatureConfig()).getAsJsonObject();
            JsonObject merged = LocalFeatureConfigDocumentPolicy.overlayKnownValues(defaults, source);
            LocalFeatureConfig loaded = GSON.fromJson(merged, LocalFeatureConfig.class);
            if (loaded == null) return false;

            if (!source.has("lavaAnalyzerHorizontalRadius")) {
                loaded.lavaAnalyzerHorizontalRadius = loaded.worksiteVisibilityHorizontalRadius;
            }
            if (!source.has("lavaAnalyzerVerticalRadius")) {
                loaded.lavaAnalyzerVerticalRadius = loaded.worksiteVisibilityVerticalRadius;
            }
            if (!source.has("lavaAnalyzerIntervalTicks")) {
                loaded.lavaAnalyzerIntervalTicks = loaded.worksiteVisibilityIntervalTicks;
            }
            if (!source.has("lavaAnalyzerMaxOverlayResults")) {
                loaded.lavaAnalyzerMaxOverlayResults = loaded.worksiteVisibilityMaxOverlayResults;
            }

            int sourceSchemaVersion = source.has("visualTargetSchemaVersion")
                    ? source.get("visualTargetSchemaVersion").getAsInt()
                    : VisualTargetSelectionPolicy.LEGACY_SCHEMA_VERSION;
            loaded.visualTargetMask = VisualTargetSelectionPolicy.migrateMask(
                    loaded.visualTargetMask,
                    sourceSchemaVersion);
            loaded.visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;

            copyFrom(loaded);
            sanitize();
            return true;
        } catch (RuntimeException failure) {
            resetToDefaults();
            return false;
        }
    }

    public synchronized boolean save() {
        sanitize();
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME, GSON.toJson(this));
            return true;
        } catch (java.io.IOException | RuntimeException error) {
            ChiseTweaksClient.LOGGER.error(
                    "Unable to save local config after {}",
                    error.getClass().getSimpleName());
            return false;
        }
    }

    void resetToDefaults() {
        copyFrom(new LocalFeatureConfig());
    }

    void sanitize() {
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)) {
            lavaHighlightEnabled = false;
        }
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.VILLAGER_ANALYZER)) {
            villagerAnalyzerEnabled = false;
        }
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BEACON_RANGE)) {
            beaconRangeEnabled = false;
        }
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LIGHTNING_ROD_RANGE)) {
            lightningRodRangeEnabled = false;
        }
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.FIRE_VISIBILITY)) {
            fireVisibilityEnabled = false;
        }
        fireVisibilitySizePreset = FireVisibilityPolicy.clampSizePreset(fireVisibilitySizePreset);
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BRIGHT_CHEST)) {
            brightChestEnabled = false;
        }
        if (!FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BRIGHT_CONCRETE)) {
            brightConcreteEnabled = false;
        }
        worksiteVisibilityHorizontalRadius =
                WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(worksiteVisibilityHorizontalRadius);
        worksiteVisibilityVerticalRadius =
                WorksiteVisibilityBudgetPolicy.clampVerticalRadius(worksiteVisibilityVerticalRadius);
        worksiteVisibilityIntervalTicks =
                WorksiteVisibilityBudgetPolicy.clampIntervalTicks(worksiteVisibilityIntervalTicks);
        worksiteVisibilityMaxOverlayResults =
                WorksiteVisibilityBudgetPolicy.clampOverlayResults(worksiteVisibilityMaxOverlayResults);
        fineThreadTraceColorPreset =
                WorksiteHighlightProfilePolicy.clampColorPreset(fineThreadTraceColorPreset);
        fineThreadTraceOpacityPercent =
                WorksiteHighlightProfilePolicy.clampOpacityPercent(fineThreadTraceOpacityPercent);
        hiddenSurfaceTraceColorPreset =
                WorksiteHighlightProfilePolicy.clampColorPreset(hiddenSurfaceTraceColorPreset);
        hiddenSurfaceTraceOpacityPercent =
                WorksiteHighlightProfilePolicy.clampOpacityPercent(hiddenSurfaceTraceOpacityPercent);
        lavaAnalyzerHorizontalRadius =
                WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(lavaAnalyzerHorizontalRadius);
        lavaAnalyzerVerticalRadius =
                WorksiteVisibilityBudgetPolicy.clampVerticalRadius(lavaAnalyzerVerticalRadius);
        lavaAnalyzerIntervalTicks =
                WorksiteVisibilityBudgetPolicy.clampIntervalTicks(lavaAnalyzerIntervalTicks);
        lavaAnalyzerMaxOverlayResults =
                WorksiteVisibilityBudgetPolicy.clampOverlayResults(lavaAnalyzerMaxOverlayResults);
        visualTargetMask = VisualTargetSelectionPolicy.sanitizeMask(visualTargetMask);
        visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    private void copyFrom(LocalFeatureConfig loaded) {
        lavaHighlightEnabled = loaded.lavaHighlightEnabled;
        villagerAnalyzerEnabled = loaded.villagerAnalyzerEnabled;
        beaconRangeEnabled = loaded.beaconRangeEnabled;
        lightningRodRangeEnabled = loaded.lightningRodRangeEnabled;
        fireVisibilityEnabled = loaded.fireVisibilityEnabled;
        fireVisibilitySizePreset = loaded.fireVisibilitySizePreset;
        brightChestEnabled = loaded.brightChestEnabled;
        brightConcreteEnabled = loaded.brightConcreteEnabled;
        oreHighlightAnimationEnabled = loaded.oreHighlightAnimationEnabled;
        interactionHistoryEnabled = loaded.interactionHistoryEnabled;
        schematicPlacementInspectorEnabled = loaded.schematicPlacementInspectorEnabled;
        worksiteVisibilityHorizontalRadius = loaded.worksiteVisibilityHorizontalRadius;
        worksiteVisibilityVerticalRadius = loaded.worksiteVisibilityVerticalRadius;
        worksiteVisibilityIntervalTicks = loaded.worksiteVisibilityIntervalTicks;
        worksiteVisibilityMaxOverlayResults = loaded.worksiteVisibilityMaxOverlayResults;
        worksiteVisibilityWorldOverlay = loaded.worksiteVisibilityWorldOverlay;
        worksiteVisibilityDimensionPresetsEnabled = loaded.worksiteVisibilityDimensionPresetsEnabled;
        fineThreadTraceColorPreset = loaded.fineThreadTraceColorPreset;
        fineThreadTraceOpacityPercent = loaded.fineThreadTraceOpacityPercent;
        hiddenSurfaceTraceColorPreset = loaded.hiddenSurfaceTraceColorPreset;
        hiddenSurfaceTraceOpacityPercent = loaded.hiddenSurfaceTraceOpacityPercent;
        lavaAnalyzerHorizontalRadius = loaded.lavaAnalyzerHorizontalRadius;
        lavaAnalyzerVerticalRadius = loaded.lavaAnalyzerVerticalRadius;
        lavaAnalyzerIntervalTicks = loaded.lavaAnalyzerIntervalTicks;
        lavaAnalyzerMaxOverlayResults = loaded.lavaAnalyzerMaxOverlayResults;
        visualTargetMask = loaded.visualTargetMask;
        visualTargetSchemaVersion = loaded.visualTargetSchemaVersion;
    }

    private static boolean needsLegacyBrightMigration(String document) {
        try {
            JsonObject root = JsonParser.parseString(document).getAsJsonObject();
            return !root.has("brightChestEnabled") || !root.has("brightConcreteEnabled");
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    /** 旧built-in packの明示的な選択状態だけを引き継ぎ、不明な状態では現在のdefault-onを維持する。 */
    private boolean migrateLegacyBrightState() {
        try {
            Optional<String> stored = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getGameDir(), "options.txt");
            if (stored.isEmpty()) return true;
            String resourcePacks = resourcePacksLine(stored.get());
            if (resourcePacks == null) return true;

            boolean legacy = selected(resourcePacks, LEGACY_PACK_ID);
            boolean chest = selected(resourcePacks, LEGACY_CHEST_PACK_ID);
            boolean concrete = selected(resourcePacks, LEGACY_CONCRETE_PACK_ID);
            if (chest || concrete) {
                brightChestEnabled = chest;
                brightConcreteEnabled = concrete;
            } else if (legacy) {
                brightChestEnabled = true;
                brightConcreteEnabled = true;
            }
            ChiseTweaksClient.LOGGER.info("Migrated legacy Bright visibility state into local feature config");
            return true;
        } catch (java.io.IOException | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Legacy Bright visibility state was not migrated after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    private static String resourcePacksLine(String document) {
        if (document == null || document.isBlank()) return null;
        for (String line : document.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("resourcePacks:")) return trimmed;
        }
        return null;
    }

    private static boolean selected(String resourcePacksLine, String packId) {
        return resourcePacksLine != null && resourcePacksLine.contains("\"" + packId + "\"");
    }
}
