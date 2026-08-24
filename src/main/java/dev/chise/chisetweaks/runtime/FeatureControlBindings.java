package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import dev.chise.chisetweaks.feature.rendering.ChunkRenderInvalidation;
import dev.chise.chisetweaks.feature.rendering.model.VisualRenderState;

public final class FeatureControlBindings {
    private FeatureControlBindings() {}

    public static void init() {
        if (builderFocusAvailable()) {
            BuilderFocusVisibility.applyConfig();
            bindBuilderFocusLists();
            bindSceneFilterRefresh();
        }
        bindModelHighlightRefresh();
    }

    private static void bindModelHighlightRefresh() {
        VisualRenderState.refreshFromConfig();
        FeatureSwitches.MATERIAL_HIGHLIGHTS.addValueChangeListener(
                ignored -> refreshVisualStateAndInvalidate());
        FeatureSwitches.KELP_HIGHLIGHT.addValueChangeListener(
                ignored -> refreshVisualStateAndInvalidate());
        FeatureSwitches.GLASS_INSPECTION.addValueChangeListener(
                ignored -> refreshVisualStateAndInvalidate());
        LocalFeatureSettings.setOreHighlightChangedCallback(
                FeatureControlBindings::refreshVisualStateAndInvalidate);
        VisualTargetSettings.setMaterialTargetsChangedCallback(
                FeatureControlBindings::refreshVisualStateAndInvalidate);
    }

    private static void refreshVisualStateAndInvalidate() {
        VisualRenderState.refreshFromConfig();
        ChunkRenderInvalidation.request();
    }

    private static void bindSceneFilterRefresh() {
        FeatureSwitches.BUILDER_FOCUS_BLOCKS.setValueChangeCallback(config ->
                BuilderFocusVisibility.buildLists());
        FeatureSwitches.BUILDER_FOCUS_ENTITIES.setValueChangeCallback(config ->
                BuilderFocusVisibility.buildEntityLists());
    }

    private static void bindBuilderFocusLists() {
        bindSanitized(BuilderFocusConfig.BLOCK_BLACKLIST, BuilderFocusVisibility::buildLists);
        bindSanitized(BuilderFocusConfig.BLOCK_WHITELIST, BuilderFocusVisibility::buildLists);
        BuilderFocusConfig.BLOCK_RULE_MODE.setValueChangeCallback(
                config -> BuilderFocusVisibility.buildLists());

        bindSanitized(BuilderFocusConfig.ENTITY_BLACKLIST, BuilderFocusVisibility::buildEntityLists);
        bindSanitized(BuilderFocusConfig.ENTITY_WHITELIST, BuilderFocusVisibility::buildEntityLists);
        BuilderFocusConfig.ENTITY_RULE_MODE.setValueChangeCallback(
                config -> BuilderFocusVisibility.buildEntityLists());
    }

    private static boolean builderFocusAvailable() {
        return FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_BLOCKS)
                || FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_ENTITIES);
    }

    private static void bindSanitized(ChiseStringListSetting config, Runnable rebuild) {
        config.setValueChangeCallback(ignored -> {
            FeatureConfig.sanitizeStringLists();
            rebuild.run();
        });
    }
}
