package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/** Local persistence for the retained bounded client-side visual settings. */
public final class LocalFeatureConfig {
    private static final LocalFeatureConfig INSTANCE = new LocalFeatureConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "chisetweaks-visual.json";

    public boolean lavaHighlightEnabled = false;
    public boolean ancientDebrisAnalyzerEnabled = false;
    public boolean fireVisibilityEnabled = false;
    public boolean oreHighlightAnimationEnabled = false;

    /** Shared bounded scan budget for the highlight-family worksite overlays. */
    public int worksiteVisibilityHorizontalRadius = 5;
    public int worksiteVisibilityVerticalRadius = 3;
    public int worksiteVisibilityIntervalTicks = 10;
    public int worksiteVisibilityMaxResults = 6;
    public int worksiteVisibilityMaxOverlayResults = 12;
    public boolean worksiteVisibilityWorldOverlay = true;
    public boolean worksiteVisibilityExclusiveMode = false;

    /** Lava Analyzer owns an independent scan budget so tuning it cannot change highlight behavior. */
    public int lavaAnalyzerHorizontalRadius = 5;
    public int lavaAnalyzerVerticalRadius = 3;
    public int lavaAnalyzerIntervalTicks = 10;
    public int lavaAnalyzerMaxOverlayResults = 12;

    /** Ancient Debris Analyzer scans loaded chunks once and only filters cached positions at runtime. */
    public int ancientDebrisAnalyzerRangeBlocks = AncientDebrisAnalyzerPolicy.DEFAULT_RANGE_BLOCKS;
    public int ancientDebrisAnalyzerMaxMarkers = AncientDebrisAnalyzerPolicy.DEFAULT_MAX_MARKERS;

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
            if (!replaceFromJsonDocument(stored.get())) {
                ChiseTweaksClient.LOGGER.warn("Rejected local config; using safe defaults");
            }
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
        lavaHighlightEnabled = false;
        ancientDebrisAnalyzerEnabled = false;
        fireVisibilityEnabled = false;
        oreHighlightAnimationEnabled = false;
        worksiteVisibilityHorizontalRadius = 5;
        worksiteVisibilityVerticalRadius = 3;
        worksiteVisibilityIntervalTicks = 10;
        worksiteVisibilityMaxResults = 6;
        worksiteVisibilityMaxOverlayResults = 12;
        worksiteVisibilityWorldOverlay = true;
        worksiteVisibilityExclusiveMode = false;
        lavaAnalyzerHorizontalRadius = 5;
        lavaAnalyzerVerticalRadius = 3;
        lavaAnalyzerIntervalTicks = 10;
        lavaAnalyzerMaxOverlayResults = 12;
        ancientDebrisAnalyzerRangeBlocks = AncientDebrisAnalyzerPolicy.DEFAULT_RANGE_BLOCKS;
        ancientDebrisAnalyzerMaxMarkers = AncientDebrisAnalyzerPolicy.DEFAULT_MAX_MARKERS;
        visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    void sanitize() {
        if (!PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)) {
            lavaHighlightEnabled = false;
        }
        if (!PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)) {
            ancientDebrisAnalyzerEnabled = false;
        }
        if (!PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.FIRE_VISIBILITY)) {
            fireVisibilityEnabled = false;
        }
        worksiteVisibilityHorizontalRadius =
                WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(worksiteVisibilityHorizontalRadius);
        worksiteVisibilityVerticalRadius =
                WorksiteVisibilityBudgetPolicy.clampVerticalRadius(worksiteVisibilityVerticalRadius);
        worksiteVisibilityIntervalTicks =
                WorksiteVisibilityBudgetPolicy.clampIntervalTicks(worksiteVisibilityIntervalTicks);
        worksiteVisibilityMaxResults =
                WorksiteVisibilityBudgetPolicy.clampLegacyResults(worksiteVisibilityMaxResults);
        worksiteVisibilityMaxOverlayResults =
                WorksiteVisibilityBudgetPolicy.clampOverlayResults(worksiteVisibilityMaxOverlayResults);
        lavaAnalyzerHorizontalRadius =
                WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(lavaAnalyzerHorizontalRadius);
        lavaAnalyzerVerticalRadius =
                WorksiteVisibilityBudgetPolicy.clampVerticalRadius(lavaAnalyzerVerticalRadius);
        lavaAnalyzerIntervalTicks =
                WorksiteVisibilityBudgetPolicy.clampIntervalTicks(lavaAnalyzerIntervalTicks);
        lavaAnalyzerMaxOverlayResults =
                WorksiteVisibilityBudgetPolicy.clampOverlayResults(lavaAnalyzerMaxOverlayResults);
        ancientDebrisAnalyzerRangeBlocks =
                AncientDebrisAnalyzerPolicy.clampRangeBlocks(ancientDebrisAnalyzerRangeBlocks);
        ancientDebrisAnalyzerMaxMarkers =
                AncientDebrisAnalyzerPolicy.clampMaxMarkers(ancientDebrisAnalyzerMaxMarkers);
        visualTargetMask = VisualTargetSelectionPolicy.sanitizeMask(visualTargetMask);
        visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    private void copyFrom(LocalFeatureConfig loaded) {
        lavaHighlightEnabled = loaded.lavaHighlightEnabled;
        ancientDebrisAnalyzerEnabled = loaded.ancientDebrisAnalyzerEnabled;
        fireVisibilityEnabled = loaded.fireVisibilityEnabled;
        oreHighlightAnimationEnabled = loaded.oreHighlightAnimationEnabled;
        worksiteVisibilityHorizontalRadius = loaded.worksiteVisibilityHorizontalRadius;
        worksiteVisibilityVerticalRadius = loaded.worksiteVisibilityVerticalRadius;
        worksiteVisibilityIntervalTicks = loaded.worksiteVisibilityIntervalTicks;
        worksiteVisibilityMaxResults = loaded.worksiteVisibilityMaxResults;
        worksiteVisibilityMaxOverlayResults = loaded.worksiteVisibilityMaxOverlayResults;
        worksiteVisibilityWorldOverlay = loaded.worksiteVisibilityWorldOverlay;
        worksiteVisibilityExclusiveMode = loaded.worksiteVisibilityExclusiveMode;
        lavaAnalyzerHorizontalRadius = loaded.lavaAnalyzerHorizontalRadius;
        lavaAnalyzerVerticalRadius = loaded.lavaAnalyzerVerticalRadius;
        lavaAnalyzerIntervalTicks = loaded.lavaAnalyzerIntervalTicks;
        lavaAnalyzerMaxOverlayResults = loaded.lavaAnalyzerMaxOverlayResults;
        ancientDebrisAnalyzerRangeBlocks = loaded.ancientDebrisAnalyzerRangeBlocks;
        ancientDebrisAnalyzerMaxMarkers = loaded.ancientDebrisAnalyzerMaxMarkers;
        visualTargetMask = loaded.visualTargetMask;
        visualTargetSchemaVersion = loaded.visualTargetSchemaVersion;
    }
}
