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
 * <p>Quarantining this engine never changes the user-facing visibility toggles.
 * The manager can stop runtime work independently from persisted preferences.</p>
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
            if (toggle.definition().isWorksiteVisibilityMode() && toggle.getBooleanValue()) return true;
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
            if (!toggle.getBooleanValue()) continue;
            var definition = toggle.definition();
            if (definition.isWorksiteVisibilityMode()) active.add(definition.inspectionCategory());
        }
        return active;
    }

    private void clear() {
        overlayRenderer.clear();
    }
}
