package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Hidden Block Highlightの対象選択と色設定を維持しつつ、Lava Analyzerと同じ
 * loaded-chunk-only / bounded / through-wall方式でHidden Blockを解析する。
 */
public final class HiddenBlockAnalyzerFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final int MAX_CANDIDATES = WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS;
    private static final int MAX_STABLE_BACKOFF_SHIFT = 2;

    private final ThroughWallMarkerRenderer renderer = new ThroughWallMarkerRenderer(
            ThroughWallMarkerRenderer.Style.HIDDEN_BLOCK,
            MAX_CANDIDATES,
            "ChiseTweaks Hidden Block Analyzer retained buffer",
            "ChiseTweaks Hidden Block Analyzer retained rendering");
    private final ThroughWallPositionSnapshot targets = new ThroughWallPositionSnapshot(MAX_CANDIDATES);
    private final ThroughWallRenderGuard renderGuard = new ThroughWallRenderGuard(
            renderer,
            targets,
            FeatureDefinition.HIDDEN_SURFACE_TRACE.id(),
            "Hidden Block Analyzer");
    private final NearestPositionBuffer nearestTargets = new NearestPositionBuffer(MAX_CANDIDATES);
    private final LevelChunk[] loadedChunkBuffer = new LevelChunk[
            WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private int ticksUntilScan;
    private int stableScanCount;
    private int lastScanFingerprint = Integer.MIN_VALUE;
    private long lastObservedPlayerBlock = Long.MIN_VALUE;
    private boolean movementSinceLastScan;
    private ClientLevel lastLevel;
    private boolean runtimeQuarantined;

    @Override
    public String getId() {
        return FeatureDefinition.HIDDEN_SURFACE_TRACE.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.HIDDEN_SURFACE_TRACE.englishName();
    }

    @Override
    public void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderGuard.close());
        ChiseTweaksClient.LOGGER.info(
                "Hidden Block Analyzer initialized with bounded loaded-chunk scanning and retained through-terrain rendering");
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            resetScanState(null);
            return;
        }
        if (isSessionQuarantined()) return;
        if (client == null || client.player == null || client.level == null) {
            resetScanState(null);
            return;
        }
        if (lastLevel != client.level) resetScanState(client.level);

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int fingerprint = scanFingerprint(local);
        int baseInterval = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.hiddenAnalyzerIntervalTicks);
        long currentPlayerBlock = client.player.blockPosition().asLong();
        if (currentPlayerBlock != lastObservedPlayerBlock) {
            lastObservedPlayerBlock = currentPlayerBlock;
            movementSinceLastScan = true;
            stableScanCount = 0;
            ticksUntilScan = Math.min(ticksUntilScan, baseInterval - 1);
        }
        if (fingerprint != lastScanFingerprint) {
            ticksUntilScan = 0;
            stableScanCount = 0;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }

        boolean changed = scanLoadedTargets(client, local);
        ticksUntilScan = nextIntervalTicks(baseInterval, changed, movementSinceLastScan) - 1;
        movementSinceLastScan = false;
        lastScanFingerprint = fingerprint;
    }

    private boolean scanLoadedTargets(Minecraft client, LocalFeatureConfig local) {
        int horizontalRadius = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.hiddenAnalyzerHorizontalRadius);
        int verticalRadius = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.hiddenAnalyzerVerticalRadius);
        int limit = WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.hiddenAnalyzerMaxOverlayResults);
        BlockPos origin = client.player.blockPosition();
        Vec3 eye = client.player.getEyePosition();

        int minX = origin.getX() - horizontalRadius;
        int maxX = origin.getX() + horizontalRadius;
        int minZ = origin.getZ() - horizontalRadius;
        int maxZ = origin.getZ() + horizontalRadius;
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        int chunkSpanX = maxChunkX - minChunkX + 1;
        int chunkCount = chunkSpanX * (maxChunkZ - minChunkZ + 1);
        if (chunkCount > loadedChunkBuffer.length) {
            nearestTargets.clear();
            return targets.clear();
        }

        int chunkIndex = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                loadedChunkBuffer[chunkIndex++] =
                        client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            }
        }

        nearestTargets.clear();
        int originY = origin.getY();
        for (int z = minZ; z <= maxZ; z++) {
            int loadedRow = ((z >> 4) - minChunkZ) * chunkSpanX;
            for (int x = minX; x <= maxX; x++) {
                int loadedIndex = loadedRow + ((x >> 4) - minChunkX);
                LevelChunk sourceChunk = loadedChunkBuffer[loadedIndex];
                if (sourceChunk == null) continue;
                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                    int y = originY + yOffset;
                    cursor.set(x, y, z);
                    BlockState state = sourceChunk.getBlockState(cursor);
                    if (BuilderFocusVisibility.shouldHide(state.getBlock())) continue;

                    Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    String blockId = id == null ? "" : id.toString();
                    if (!BlockInspectionPolicy.matches(
                            blockId, BlockInspectionCategory.HIDDEN_SURFACE)) continue;
                    if (!VisualTargetSelectionPolicy.matchesEnabled(
                            local.visualTargetMask,
                            blockId,
                            BlockInspectionCategory.HIDDEN_SURFACE)) continue;

                    double dx = x + 0.5 - eye.x;
                    double dy = y + 0.5 - eye.y;
                    double dz = z + 0.5 - eye.z;
                    nearestTargets.offer(
                            BlockPos.asLong(x, y, z),
                            dx * dx + dy * dy + dz * dz,
                            limit);
                }
            }
        }

        nearestTargets.sortPositions();
        return targets.publish(
                nearestTargets.positions(),
                nearestTargets.count(),
                eye.x,
                eye.y,
                eye.z);
    }

    private int nextIntervalTicks(int baseInterval, boolean changed, boolean movedSinceLastScan) {
        if (changed || movedSinceLastScan) {
            stableScanCount = 0;
        } else {
            stableScanCount = Math.min(stableScanCount + 1, MAX_STABLE_BACKOFF_SHIFT);
        }
        return Math.min(
                WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
                baseInterval * (1 << stableScanCount));
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled() || targets.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || client.level != lastLevel
                || client.screen != null) return;
        renderGuard.render(context);
    }

    private void resetScanState(ClientLevel level) {
        targets.clear();
        nearestTargets.clear();
        ticksUntilScan = 0;
        stableScanCount = 0;
        lastScanFingerprint = Integer.MIN_VALUE;
        lastObservedPlayerBlock = Long.MIN_VALUE;
        movementSinceLastScan = false;
        lastLevel = level;
    }

    private static int scanFingerprint(LocalFeatureConfig local) {
        int result = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.hiddenAnalyzerHorizontalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.hiddenAnalyzerVerticalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.hiddenAnalyzerIntervalTicks);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.hiddenAnalyzerMaxOverlayResults);
        result = 31 * result + local.visualTargetMask;
        result = 31 * result + local.hiddenSurfaceTraceColorPreset;
        result = 31 * result + local.hiddenSurfaceTraceOpacityPercent;
        return result;
    }

    private boolean isSessionQuarantined() {
        return runtimeQuarantined || renderGuard.isQuarantined();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        runtimeQuarantined = true;
        resetScanState(null);
        renderGuard.close();
    }

    @Override
    public void resetSession(Minecraft client) {
        resetScanState(null);
        renderGuard.resetSession();
    }

    @Override
    public boolean isEnabled() {
        return !isSessionQuarantined()
                && FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue();
    }
}
