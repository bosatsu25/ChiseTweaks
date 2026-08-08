package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/** Local persistence for bounded client-side ChiseTweaks settings. */
public final class LocalFeatureConfig {
    private static final LocalFeatureConfig INSTANCE = new LocalFeatureConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "chisetweaks-visual.json";

    public boolean lavaHighlightEnabled = false;
    public boolean lavaHighlightSource = true;
    public boolean lavaHighlightFlowing = true;
    public int lavaSourceColor = 0xFFFF3B30;
    public int lavaFlowingColor = 0xFFFF9500;

    public int worksiteVisibilityHorizontalRadius = 5;
    public int worksiteVisibilityVerticalRadius = 3;
    public int worksiteVisibilityIntervalTicks = 10;
    public int worksiteVisibilityMaxResults = 6;
    public int worksiteVisibilityMaxOverlayResults = 12;
    public boolean worksiteVisibilityWorldOverlay = true;
    public boolean worksiteVisibilityExclusiveMode = false;

    public int pumpkinScaffoldPlacementRange = PumpkinScaffoldPolicy.DEFAULT_PLACEMENT_RANGE;

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

    /** Replaces current values only when the complete document passes security validation. */
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
            copyFrom(loaded);
            sanitize();
            return true;
        } catch (RuntimeException failure) {
            resetToDefaults();
            return false;
        }
    }

    public synchronized void save() {
        sanitize();
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), CONFIG_FILE_NAME, GSON.toJson(this));
        } catch (java.io.IOException | RuntimeException error) {
            ChiseTweaksClient.LOGGER.error(
                    "Unable to save local config after {}",
                    error.getClass().getSimpleName());
        }
    }

    void resetToDefaults() {
        lavaHighlightEnabled = false;
        lavaHighlightSource = true;
        lavaHighlightFlowing = true;
        lavaSourceColor = 0xFFFF3B30;
        lavaFlowingColor = 0xFFFF9500;

        worksiteVisibilityHorizontalRadius = 5;
        worksiteVisibilityVerticalRadius = 3;
        worksiteVisibilityIntervalTicks = 10;
        worksiteVisibilityMaxResults = 6;
        worksiteVisibilityMaxOverlayResults = 12;
        worksiteVisibilityWorldOverlay = true;
        worksiteVisibilityExclusiveMode = false;
        pumpkinScaffoldPlacementRange = PumpkinScaffoldPolicy.DEFAULT_PLACEMENT_RANGE;
    }

    void sanitize() {
        worksiteVisibilityHorizontalRadius =
                WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(worksiteVisibilityHorizontalRadius);
        worksiteVisibilityVerticalRadius =
                WorksiteVisibilityBudgetPolicy.clampVerticalRadius(worksiteVisibilityVerticalRadius);
        worksiteVisibilityIntervalTicks =
                WorksiteVisibilityBudgetPolicy.clampIntervalTicks(worksiteVisibilityIntervalTicks);
        worksiteVisibilityMaxResults =
                WorksiteVisibilityBudgetPolicy.clampHudResults(worksiteVisibilityMaxResults);
        worksiteVisibilityMaxOverlayResults =
                WorksiteVisibilityBudgetPolicy.clampOverlayResults(worksiteVisibilityMaxOverlayResults);
        pumpkinScaffoldPlacementRange =
                PumpkinScaffoldPolicy.clampPlacementRange(pumpkinScaffoldPlacementRange);
    }

    private void copyFrom(LocalFeatureConfig loaded) {
        lavaHighlightEnabled = loaded.lavaHighlightEnabled;
        lavaHighlightSource = loaded.lavaHighlightSource;
        lavaHighlightFlowing = loaded.lavaHighlightFlowing;
        lavaSourceColor = loaded.lavaSourceColor;
        lavaFlowingColor = loaded.lavaFlowingColor;

        worksiteVisibilityHorizontalRadius = loaded.worksiteVisibilityHorizontalRadius;
        worksiteVisibilityVerticalRadius = loaded.worksiteVisibilityVerticalRadius;
        worksiteVisibilityIntervalTicks = loaded.worksiteVisibilityIntervalTicks;
        worksiteVisibilityMaxResults = loaded.worksiteVisibilityMaxResults;
        worksiteVisibilityMaxOverlayResults = loaded.worksiteVisibilityMaxOverlayResults;
        worksiteVisibilityWorldOverlay = loaded.worksiteVisibilityWorldOverlay;
        worksiteVisibilityExclusiveMode = loaded.worksiteVisibilityExclusiveMode;
        pumpkinScaffoldPlacementRange = loaded.pumpkinScaffoldPlacementRange;
    }
}
