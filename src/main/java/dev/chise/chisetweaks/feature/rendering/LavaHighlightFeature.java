package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class LavaHighlightFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final int MAX_CANDIDATES = WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS;
    private static final int MAX_STABLE_BACKOFF_SHIFT = 2;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ThroughWallMarkerRenderer sourceRenderer = new ThroughWallMarkerRenderer(
            ThroughWallMarkerRenderer.Style.LAVA_SOURCE,
            MAX_CANDIDATES,
            "ChiseTweaks Lava Source Highlight retained buffer",
            "ChiseTweaks Lava Source Highlight retained rendering");
    private final ThroughWallPositionSnapshot highlightedSources = new ThroughWallPositionSnapshot(MAX_CANDIDATES);
    private final ThroughWallRenderGuard renderGuard = new ThroughWallRenderGuard(
            sourceRenderer,
            highlightedSources,
            FeatureDefinition.LAVA_HIGHLIGHT.id(),
            "Lava Source Highlight");
    private final NearestPositionBuffer nearestSources = new NearestPositionBuffer(MAX_CANDIDATES);
    private final LevelChunk[] loadedChunkBuffer = new LevelChunk[
            WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();

    private int ticksUntilScan;
    private int stableScanCount;
    private int lastScanFingerprint = Integer.MIN_VALUE;
    private long lastObservedPlayerBlock = Long.MIN_VALUE;
    private boolean movementSinceLastScan;
    private ClientLevel lastLevel;
    private boolean runtimeQuarantined;

    @Override
    public String getId() {
        return FeatureDefinition.LAVA_HIGHLIGHT.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.LAVA_HIGHLIGHT.englishName();
    }

    @Override
    public void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderGuard.close());
        ChiseTweaksClient.LOGGER.info(
                "Lava Source Highlight initialized with adaptive bounded scanning and retained through-terrain rendering");
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

        if (lastLevel != client.level) {
            resetScanState(client.level);
        }

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int fingerprint = scanFingerprint(local);
        int baseInterval = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.lavaAnalyzerIntervalTicks);
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

        boolean sourcesChanged = scanLoadedSources(client, local);
        ticksUntilScan = nextIntervalTicks(baseInterval, sourcesChanged, movementSinceLastScan) - 1;
        movementSinceLastScan = false;
        lastScanFingerprint = fingerprint;
    }

    private boolean scanLoadedSources(Minecraft client, LocalFeatureConfig local) {
        int horizontalRadius = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.lavaAnalyzerHorizontalRadius);
        int verticalRadius = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.lavaAnalyzerVerticalRadius);
        int limit = WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.lavaAnalyzerMaxOverlayResults);
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
            nearestSources.clear();
            return highlightedSources.clear();
        }

        int chunkIndex = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                // getChunkNow() はチャンクを新規ロードしない。getChunk(..., true) へ置き換えてはならない。
                loadedChunkBuffer[chunkIndex++] = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            }
        }

        nearestSources.clear();
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
                    FluidState fluidState = sourceChunk.getFluidState(cursor);
                    boolean source = isSourceLava(fluidState);
                    boolean boundary = source && hasKnownSourceBoundary(client, sourceChunk, cursor);
                    if (!LavaVisionPalettePolicy.shouldHighlight(true, source, boundary)) continue;

                    double dx = x + 0.5 - eye.x;
                    double dy = y + 0.5 - eye.y;
                    double dz = z + 0.5 - eye.z;
                    nearestSources.offer(
                            BlockPos.asLong(x, y, z),
                            dx * dx + dy * dy + dz * dz,
                            limit);
                }
            }
        }

        nearestSources.sortPositions();
        return highlightedSources.publish(
                nearestSources.positions(),
                nearestSources.count(),
                eye.x,
                eye.y,
                eye.z);
    }

    /**
     * 未ロードの水平方向隣接チャンクは空気や非源泉とみなさず、不明として扱う。これによりチャンク境界でもロード済み情報だけで判定できる。
     */
    private boolean hasKnownSourceBoundary(
            Minecraft client,
            LevelChunk sourceChunk,
            BlockPos position) {
        int sourceChunkX = position.getX() >> 4;
        int sourceChunkZ = position.getZ() >> 4;
        for (Direction direction : DIRECTIONS) {
            neighborCursor.set(
                    position.getX() + direction.getStepX(),
                    position.getY() + direction.getStepY(),
                    position.getZ() + direction.getStepZ());
            int neighborChunkX = neighborCursor.getX() >> 4;
            int neighborChunkZ = neighborCursor.getZ() >> 4;
            LevelChunk neighborChunk = sourceChunk;
            if (neighborChunkX != sourceChunkX || neighborChunkZ != sourceChunkZ) {
                neighborChunk = client.level.getChunkSource().getChunkNow(neighborChunkX, neighborChunkZ);
                if (neighborChunk == null) continue;
            }
            if (!isSourceLava(neighborChunk.getFluidState(neighborCursor))) return true;
        }
        return false;
    }

    private static boolean isSourceLava(FluidState fluidState) {
        if (fluidState == null || !fluidState.isSource()) return false;
        return fluidState.getType() == Fluids.LAVA || fluidState.getType() == Fluids.FLOWING_LAVA;
    }

    private int nextIntervalTicks(int baseInterval, boolean sourcesChanged, boolean movedSinceLastScan) {
        if (sourcesChanged || movedSinceLastScan) {
            stableScanCount = 0;
        } else {
            stableScanCount = Math.min(stableScanCount + 1, MAX_STABLE_BACKOFF_SHIFT);
        }
        int multiplier = 1 << stableScanCount;
        return Math.min(
                WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
                baseInterval * multiplier);
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled()
                || highlightedSources.isEmpty()
                || BuilderFocusVisibility.shouldHide(Blocks.LAVA)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || client.level != lastLevel
                || client.screen != null) return;
        renderGuard.render(context);
    }

    private void clearTargets() {
        highlightedSources.clear();
        nearestSources.clear();
    }

    private void resetScanState(ClientLevel level) {
        clearTargets();
        ticksUntilScan = 0;
        stableScanCount = 0;
        lastScanFingerprint = Integer.MIN_VALUE;
        lastObservedPlayerBlock = Long.MIN_VALUE;
        movementSinceLastScan = false;
        lastLevel = level;
    }

    private static int scanFingerprint(LocalFeatureConfig local) {
        int result = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.lavaAnalyzerHorizontalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.lavaAnalyzerVerticalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.lavaAnalyzerIntervalTicks);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.lavaAnalyzerMaxOverlayResults);
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
        // 描画経路の失敗はワールドやセッション初期化中だけの一過性である場合がある。Manager側の隔離状態はプロセス全体で保持するため、ここではリセットしない。
        renderGuard.resetSession();
    }

    @Override
    public boolean isEnabled() {
        return FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)
                && !isSessionQuarantined()
                && LocalFeatureConfig.getInstance().lavaHighlightEnabled;
    }
}
