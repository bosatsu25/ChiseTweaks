package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
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
    public boolean oreHighlightAnimationEnabled = false;

    public int worksiteVisibilityHorizontalRadius = 5;
    public int worksiteVisibilityVerticalRadius = 3;
    public int worksiteVisibilityIntervalTicks = 10;
    public int worksiteVisibilityMaxResults = 6;
    public int worksiteVisibilityMaxOverlayResults = 12;
    public boolean worksiteVisibilityWorldOverlay = true;
    public boolean worksiteVisibilityExclusiveMode = false;

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
        oreHighlightAnimationEnabled = false;
        worksiteVisibilityHorizontalRadius = 5;
        worksiteVisibilityVerticalRadius = 3;
        worksiteVisibilityIntervalTicks = 10;
        worksiteVisibilityMaxResults = 6;
        worksiteVisibilityMaxOverlayResults = 12;
        worksiteVisibilityWorldOverlay = true;
        worksiteVisibilityExclusiveMode = false;
        visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    void sanitize() {
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
        visualTargetMask = VisualTargetSelectionPolicy.sanitizeMask(visualTargetMask);
        visualTargetSchemaVersion = VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION;
    }

    private void copyFrom(LocalFeatureConfig loaded) {
        lavaHighlightEnabled = loaded.lavaHighlightEnabled;
        oreHighlightAnimationEnabled = loaded.oreHighlightAnimationEnabled;
        worksiteVisibilityHorizontalRadius = loaded.worksiteVisibilityHorizontalRadius;
        worksiteVisibilityVerticalRadius = loaded.worksiteVisibilityVerticalRadius;
        worksiteVisibilityIntervalTicks = loaded.worksiteVisibilityIntervalTicks;
        worksiteVisibilityMaxResults = loaded.worksiteVisibilityMaxResults;
        worksiteVisibilityMaxOverlayResults = loaded.worksiteVisibilityMaxOverlayResults;
        worksiteVisibilityWorldOverlay = loaded.worksiteVisibilityWorldOverlay;
        worksiteVisibilityExclusiveMode = loaded.worksiteVisibilityExclusiveMode;
        visualTargetMask = loaded.visualTargetMask;
        visualTargetSchemaVersion = loaded.visualTargetSchemaVersion;
    }
}
