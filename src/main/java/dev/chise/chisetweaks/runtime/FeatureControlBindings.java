package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightRenderInvalidation;
import dev.chise.chisetweaks.feature.rendering.model.VisualRenderState;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Callback wiring for Chise-owned feature toggles and bounded mode coordination. */
public final class FeatureControlBindings {
    private static final List<FeatureSwitch> WORKSITE_VISIBILITY_TOGGLES =
            FeatureSwitches.VALUES.stream()
                    .filter(toggle -> toggle.definition().isWorksiteVisibilityMode())
                    .filter(toggle -> PreReleaseFeaturePolicy.isAvailable(toggle.definition()))
                    .toList();
    private static boolean applyingExclusiveWorksiteSelection;

    private FeatureControlBindings() {}

    public static void init() {
        if (!WORKSITE_VISIBILITY_TOGGLES.isEmpty()) bindWorksiteVisibilityCallbacks();
        if (builderFocusAvailable()) {
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
        OreHighlightRenderInvalidation.request();
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

    private static void bindWorksiteVisibilityCallbacks() {
        for (FeatureSwitch toggle : worksiteVisibilityToggles()) {
            bindExclusiveWorksiteMode(toggle);
        }
        LocalFeatureSettings.setWorksiteVisibilityModeChangedCallback(
                FeatureControlBindings::normalizeExclusiveWorksiteMode);
        normalizeExclusiveWorksiteMode();
    }

    private static void bindExclusiveWorksiteMode(FeatureSwitch selected) {
        selected.setValueChangeCallback(config -> {
            if (applyingExclusiveWorksiteSelection) return;
            WorksiteVisibilitySelectionPolicy.Mode selectedMode = modeOf(selected);
            Set<WorksiteVisibilitySelectionPolicy.Mode> nextModes =
                    WorksiteVisibilitySelectionPolicy.afterToggle(
                            activeWorksiteModes(),
                            selectedMode,
                            config.getBooleanValue(),
                            LocalFeatureConfig.getInstance().worksiteVisibilityExclusiveMode);
            applyingExclusiveWorksiteSelection = true;
            try {
                applyWorksiteModes(nextModes);
            } finally {
                applyingExclusiveWorksiteSelection = false;
            }
        });
    }

    private static void normalizeExclusiveWorksiteMode() {
        Set<WorksiteVisibilitySelectionPolicy.Mode> normalizedModes =
                WorksiteVisibilitySelectionPolicy.normalize(
                        activeWorksiteModes(),
                        LocalFeatureConfig.getInstance().worksiteVisibilityExclusiveMode);
        applyingExclusiveWorksiteSelection = true;
        try {
            applyWorksiteModes(normalizedModes);
        } finally {
            applyingExclusiveWorksiteSelection = false;
        }
    }

    private static boolean builderFocusAvailable() {
        return PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_BLOCKS)
                || PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_ENTITIES);
    }

    private static List<FeatureSwitch> worksiteVisibilityToggles() {
        return WORKSITE_VISIBILITY_TOGGLES;
    }

    private static Set<WorksiteVisibilitySelectionPolicy.Mode> activeWorksiteModes() {
        EnumSet<WorksiteVisibilitySelectionPolicy.Mode> active =
                EnumSet.noneOf(WorksiteVisibilitySelectionPolicy.Mode.class);
        for (FeatureSwitch toggle : worksiteVisibilityToggles()) {
            if (toggle.getBooleanValue()) active.add(modeOf(toggle));
        }
        return active;
    }

    private static void applyWorksiteModes(Set<WorksiteVisibilitySelectionPolicy.Mode> activeModes) {
        for (FeatureSwitch toggle : worksiteVisibilityToggles()) {
            toggle.setBooleanValue(activeModes.contains(modeOf(toggle)));
        }
    }

    private static WorksiteVisibilitySelectionPolicy.Mode modeOf(FeatureSwitch toggle) {
        WorksiteVisibilitySelectionPolicy.Mode mode = toggle.definition().worksiteMode();
        if (mode == null) throw new IllegalArgumentException("Not a scan visibility toggle: " + toggle);
        return mode;
    }

    private static void bindSanitized(ChiseStringListSetting config, Runnable rebuild) {
        config.setValueChangeCallback(ignored -> {
            FeatureConfig.sanitizeStringLists();
            rebuild.run();
        });
    }
}
