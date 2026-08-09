package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteScanThrottlePolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.EnumSet;
import java.util.List;

/**
 * Coordinates bounded worksite world overlays without owning user feature state.
 *
 * <p>Material Highlights are model-backed and deliberately excluded from this bounded scan path.
 * Active state is cached once per client tick so the render callback never walks every feature
 * switch. Stationary players use a slower periodic refresh while movement keeps the configured
 * scan cadence.</p>
 */
public final class WorksiteVisibilityEngine
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    public static final String RUNTIME_ID = "worksite_visibility_engine";
    private static final BlockInspectionCategory[] INSPECTION_CATEGORIES =
            BlockInspectionCategory.values();

    private final WorksiteBlockInspector blockInspector = new WorksiteBlockInspector();
    private final WorksiteScanner scanner = new WorksiteScanner(blockInspector);
    private final WorksiteOverlayRenderer overlayRenderer = new WorksiteOverlayRenderer(this::isActive);

    private volatile boolean active;
    private int ticksSinceLastScan;
    private int lastScanFingerprint = Integer.MIN_VALUE;
    private long lastObservedPlayerPos = Long.MIN_VALUE;
    private boolean movementObserved;
    private ClientLevel lastLevel;

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
        return active;
    }

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            deactivateAndReset();
            return;
        }

        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        int categoryMask = activeCategoryMask();
        boolean nowActive = categoryMask != 0 && config.worksiteVisibilityWorldOverlay;
        if (!nowActive) {
            deactivateAndReset();
            return;
        }
        active = true;

        long currentPlayerPos = client.player.blockPosition().asLong();
        if (lastObservedPlayerPos != Long.MIN_VALUE
                && currentPlayerPos != lastObservedPlayerPos) {
            movementObserved = true;
        }
        lastObservedPlayerPos = currentPlayerPos;

        if (ticksSinceLastScan < Integer.MAX_VALUE) ticksSinceLastScan++;
        int fingerprint = scanFingerprint(config, categoryMask);
        boolean urgentChange = lastLevel != client.level
                || lastScanFingerprint == Integer.MIN_VALUE
                || lastScanFingerprint != fingerprint;
        if (!WorksiteScanThrottlePolicy.shouldScan(
                ticksSinceLastScan,
                config.worksiteVisibilityIntervalTicks,
                urgentChange,
                movementObserved)) {
            return;
        }

        EnumSet<BlockInspectionCategory> activeCategories = categoriesFromMask(categoryMask);
        List<WorksiteVisibleTarget> targets = scanner.scan(client, config, activeCategories);
        overlayRenderer.updateTargets(targets);
        lastLevel = client.level;
        lastScanFingerprint = fingerprint;
        ticksSinceLastScan = 0;
        movementObserved = false;
    }

    @Override
    public void resetSession(Minecraft client) {
        deactivateAndReset();
    }

    private int activeCategoryMask() {
        int mask = 0;
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            if (!toggle.getBooleanValue() || !usesWorldOverlay(toggle)) continue;
            BlockInspectionCategory category = toggle.definition().inspectionCategory();
            mask |= 1 << category.ordinal();
        }
        return mask;
    }

    private static EnumSet<BlockInspectionCategory> categoriesFromMask(int mask) {
        EnumSet<BlockInspectionCategory> result = EnumSet.noneOf(BlockInspectionCategory.class);
        for (BlockInspectionCategory category : INSPECTION_CATEGORIES) {
            if ((mask & (1 << category.ordinal())) != 0) result.add(category);
        }
        return result;
    }

    private static int scanFingerprint(LocalFeatureConfig config, int categoryMask) {
        int result = categoryMask;
        result = 31 * result + config.visualTargetMask;
        result = 31 * result + config.worksiteVisibilityHorizontalRadius;
        result = 31 * result + config.worksiteVisibilityVerticalRadius;
        result = 31 * result + config.worksiteVisibilityMaxOverlayResults;
        return result;
    }

    private static boolean usesWorldOverlay(FeatureSwitch toggle) {
        var definition = toggle.definition();
        return definition.isWorksiteVisibilityMode()
                && definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT;
    }

    private void deactivateAndReset() {
        if (active) overlayRenderer.clear();
        active = false;
        ticksSinceLastScan = 0;
        lastScanFingerprint = Integer.MIN_VALUE;
        lastObservedPlayerPos = Long.MIN_VALUE;
        movementObserved = false;
        lastLevel = null;
    }
}
