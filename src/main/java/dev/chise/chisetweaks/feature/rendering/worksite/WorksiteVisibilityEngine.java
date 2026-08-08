package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.minecraft.client.Minecraft;

import java.util.EnumSet;
import java.util.List;

/**
 * Coordinates bounded worksite world overlays without owning user feature state.
 *
 * <p>Material Highlights are model-backed and deliberately excluded from this bounded scan path.
 * The remaining visibility modes keep their existing line-of-sight and result-budget behavior.</p>
 */
public final class WorksiteVisibilityEngine
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    public static final String RUNTIME_ID = "worksite_visibility_engine";

    private final WorksiteBlockInspector blockInspector = new WorksiteBlockInspector();
    private final WorksiteScanner scanner = new WorksiteScanner(blockInspector);
    private final WorksiteOverlayRenderer overlayRenderer = new WorksiteOverlayRenderer(this::isActive);
    private int ticksUntilScan;

    @Override
    public String getId() {
        return RUNTIME_ID;
    }

    @Override
    public void init() {
        overlayRenderer.init();
    }

    @Override
    public boolean isActive() {
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            if (usesWorldOverlay(toggle) && toggle.getBooleanValue()) return true;
        }
        return false;
    }

    @Override
    public void tick(Minecraft client) {
        if (!isActive() || client.player == null || client.level == null) {
            clear();
            ticksUntilScan = 0;
            return;
        }

        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        if (ticksUntilScan-- > 0) return;
        ticksUntilScan = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                config.worksiteVisibilityIntervalTicks) - 1;

        EnumSet<BlockInspectionCategory> activeCategories = activeCategories();
        List<WorksiteVisibleTarget> targets = scanner.scan(client, config, activeCategories);
        overlayRenderer.updateTargets(targets);
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
        ticksUntilScan = 0;
    }

    private EnumSet<BlockInspectionCategory> activeCategories() {
        EnumSet<BlockInspectionCategory> active = EnumSet.noneOf(BlockInspectionCategory.class);
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            if (!toggle.getBooleanValue() || !usesWorldOverlay(toggle)) continue;
            active.add(toggle.definition().inspectionCategory());
        }
        return active;
    }

    private static boolean usesWorldOverlay(FeatureSwitch toggle) {
        var definition = toggle.definition();
        return definition.isWorksiteVisibilityMode()
                && definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT;
    }

    private void clear() {
        overlayRenderer.clear();
    }
}
