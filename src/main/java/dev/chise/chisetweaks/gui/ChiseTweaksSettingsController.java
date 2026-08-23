package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChestVisibilitySetting;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.config.WhiteConcreteVisibilitySetting;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticSnapshot;

import java.util.ArrayList;
import java.util.List;

/** 設定操作だけを調整し、表示用メタデータは{@link ChiseTweaksSettingsCatalog}へ分離するcontroller。 */
final class ChiseTweaksSettingsController {
    enum Surface {
        MAIN,
        HIGHLIGHT_DETAILS,
        VISUAL_FILTER_DETAILS,
        LAVA_DETAILS
    }

    private final ChiseTweaksSettingsCatalog catalog = new ChiseTweaksSettingsCatalog();

    static ChiseTweaksSettingsController forCurrentLanguage() {
        return new ChiseTweaksSettingsController();
    }

    void initialize() {
        LocalFeatureSettings.init();
        VisualTargetSettings.init();
    }

    List<ChiseTweaksSettingRowDefinition> rows() {
        return rows(Surface.MAIN);
    }

    List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        Surface resolved = surface == null ? Surface.MAIN : surface;
        List<ChiseTweaksSettingRowDefinition> base = catalog.rows(resolved);
        if (resolved != Surface.MAIN) return base;
        ArrayList<ChiseTweaksSettingRowDefinition> result = new ArrayList<>(base.size() + 4);
        result.addAll(base);
        result.add(ChiseTweaksSettingRowDefinition.header(
                "header.diagnostics", "Diagnostics / 診断"));
        result.add(ChiseTweaksSettingRowDefinition.header(
                "diagnosticReloadState",
                "Resource reload: " + RuntimeDiagnosticSnapshot.reloadState(
                        ChiseTexturePackController.isReloadInFlight(),
                        ChiseTexturePackController.hasPendingRecovery())));
        result.add(ChiseTweaksSettingRowDefinition.action(
                "copyDiagnostics",
                "Copy Diagnostic Snapshot / 診断情報をコピー",
                "Copy a privacy-minimized runtime snapshot to the clipboard / 個人情報を追加しない実行状態をクリップボードへコピーします",
                ChiseTweaksSettingRowDefinition.Action.COPY_DIAGNOSTICS,
                "Copy / コピー"));
        result.add(ChiseTweaksSettingRowDefinition.action(
                "exportDiagnostics",
                "Export Diagnostic Snapshot / 診断情報を書き出す",
                "Save a bounded local diagnostic report without server address or absolute paths / サーバーアドレスや絶対パスを含めず診断情報を保存します",
                ChiseTweaksSettingRowDefinition.Action.EXPORT_DIAGNOSTICS,
                "Export / 保存"));
        return List.copyOf(result);
    }

    String surfaceTitle(Surface surface) {
        return catalog.surfaceTitle(surface);
    }

    boolean reset(Surface surface) {
        switch (surface == null ? Surface.MAIN : surface) {
            case MAIN -> resetAll();
            case HIGHLIGHT_DETAILS -> resetHighlightDetails();
            case VISUAL_FILTER_DETAILS -> resetBuilderFocusDetails();
            case LAVA_DETAILS -> resetAnalyzerDetails();
        }
        return true;
    }

    boolean resetAll() {
        FeatureSwitches.VALUES.forEach(ChiseBooleanSetting::resetToDefault);
        LocalFeatureSwitches.VALUES.forEach(ChiseBooleanSetting::resetToDefault);
        ChestVisibilitySetting.INSTANCE.resetToDefault();
        WhiteConcreteVisibilitySetting.INSTANCE.resetToDefault();
        resetHighlightDetails();
        resetAnalyzerDetails();
        resetBuilderFocusDetails();
        return true;
    }

    boolean saveConfig() {
        boolean featureSaved = FeatureConfig.saveToFile();
        boolean localSaved = LocalFeatureConfig.getInstance().save();
        return featureSaved && localSaved;
    }

    private void resetHighlightDetails() {
        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_DIMENSION_PRESETS.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.FINE_THREAD_TRACE_OPACITY.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_COLOR_PRESET.resetToDefault();
        LocalFeatureSettings.HIDDEN_SURFACE_TRACE_OPACITY.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.TECHNICAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
    }

    private void resetAnalyzerDetails() {
        LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_INTERVAL.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE.resetToDefault();
        LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS.resetToDefault();
    }

    private void resetBuilderFocusDetails() {
        BuilderFocusConfig.REFRESH_RENDERER.resetToDefault();
        BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault();
        BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault();
        BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault();
        BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault();
        BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault();
        BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault();
    }

    private static void resetTargetGroup(VisualTargetGroupPolicy.Group group) {
        if (group == VisualTargetGroupPolicy.Group.MATERIAL) {
            VisualTargetSettings.setAllOreHighlightTargets(true);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(config.visualTargetMask, group, true);
    }
}
