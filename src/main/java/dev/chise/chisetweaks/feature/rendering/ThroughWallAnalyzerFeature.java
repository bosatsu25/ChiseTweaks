package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Lava Analyzer / Hidden Block Analyzer のloaded-chunk走査とsession stateを共有する内部runtime。
 * user-facing toggle・scan budget・target buffer・render guardは独立したまま維持する。
 */
public final class ThroughWallAnalyzerFeature
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    private static final int LAVA_INDEX = 0;
    private static final int HIDDEN_INDEX = 1;
    private static final int LAVA_MASK = 1;
    private static final int HIDDEN_MASK = 1 << 1;
    private static final int MAX_CANDIDATES = WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS;
    private static final int MAX_STABLE_BACKOFF_SHIFT = 2;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ThroughWallPositionSnapshot lavaTargets = new ThroughWallPositionSnapshot(MAX_CANDIDATES);
    private final ThroughWallPositionSnapshot hiddenTargets = new ThroughWallPositionSnapshot(MAX_CANDIDATES);
    private final ThroughWallRenderGuard lavaRenderGuard = new ThroughWallRenderGuard(
            new ThroughWallMarkerRenderer(
                    ThroughWallMarkerRenderer.Style.LAVA_SOURCE,
                    MAX_CANDIDATES,
                    "ChiseTweaks Lava Source Highlight retained buffer",
                    "ChiseTweaks Lava Source Highlight retained rendering"),
            lavaTargets,
            FeatureDefinition.LAVA_HIGHLIGHT.id(),
            "Lava Source Highlight");
    private final ThroughWallRenderGuard hiddenRenderGuard = new ThroughWallRenderGuard(
            new ThroughWallMarkerRenderer(
                    ThroughWallMarkerRenderer.Style.HIDDEN_BLOCK,
                    MAX_CANDIDATES,
                    "ChiseTweaks Hidden Block Analyzer retained buffer",
                    "ChiseTweaks Hidden Block Analyzer retained rendering"),
            hiddenTargets,
            FeatureDefinition.HIDDEN_SURFACE_TRACE.id(),
            "Hidden Block Analyzer");
    private final NearestPositionBuffer lavaNearest = new NearestPositionBuffer(MAX_CANDIDATES);
    private final NearestPositionBuffer hiddenNearest = new NearestPositionBuffer(MAX_CANDIDATES);
    private final LoadedChunkWindow loadedChunks =
            new LoadedChunkWindow(WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES);
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();
    private final int[] ticksUntilScan = new int[2];
    private final int[] stableScanCount = new int[2];
    private final int[] lastScanFingerprint = {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private final boolean[] movementSinceLastScan = new boolean[2];
    private final java.util.IdentityHashMap<net.minecraft.world.level.block.Block, Integer> hiddenTargetMasks =
            new java.util.IdentityHashMap<>();

    private long lastObservedPlayerBlock = Long.MIN_VALUE;
    private ClientLevel lastLevel;
    private int lastEnabledMask;
    private boolean runtimeQuarantined;

    @Override
    public String getId() {
        return "through_wall_analyzers";
    }

    @Override
    public void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> closeRenderers());
        ChiseTweaksClient.LOGGER.info(
                "Through-wall analyzers initialized with shared loaded-chunk traversal and independent budgets");
    }

    @Override
    public void tick(Minecraft client) {
        if (runtimeQuarantined) return;
        if (client == null || client.player == null || client.level == null) {
            resetScanState(null);
            return;
        }
        if (lastLevel != client.level) resetScanState(client.level);

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int enabledMask = enabledMask();
        syncEnabledMask(enabledMask);
        if (enabledMask == 0) {
            lastObservedPlayerBlock = Long.MIN_VALUE;
            return;
        }

        int lavaInterval = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.lavaAnalyzerIntervalTicks);
        int hiddenInterval = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.hiddenAnalyzerIntervalTicks);
        long currentPlayerBlock = client.player.blockPosition().asLong();
        if (currentPlayerBlock != lastObservedPlayerBlock) {
            lastObservedPlayerBlock = currentPlayerBlock;
            observeMovement(enabledMask, lavaInterval, hiddenInterval);
        }

        int dueMask = 0;
        if ((enabledMask & LAVA_MASK) != 0) {
            dueMask |= updateDueState(
                    LAVA_INDEX,
                    LAVA_MASK,
                    lavaInterval,
                    lavaFingerprint(local));
        }
        if ((enabledMask & HIDDEN_MASK) != 0) {
            dueMask |= updateDueState(
                    HIDDEN_INDEX,
                    HIDDEN_MASK,
                    hiddenInterval,
                    hiddenFingerprint(local));
        }
        if (dueMask == 0) return;

        int changedMask = scanLoadedTargets(client, local, dueMask);
        if ((dueMask & LAVA_MASK) != 0) {
            finishScan(
                    LAVA_INDEX,
                    lavaInterval,
                    (changedMask & LAVA_MASK) != 0,
                    lavaFingerprint(local));
        }
        if ((dueMask & HIDDEN_MASK) != 0) {
            finishScan(
                    HIDDEN_INDEX,
                    hiddenInterval,
                    (changedMask & HIDDEN_MASK) != 0,
                    hiddenFingerprint(local));
        }
    }

    private int scanLoadedTargets(Minecraft client, LocalFeatureConfig local, int dueMask) {
        int lavaHorizontal = (dueMask & LAVA_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(local.lavaAnalyzerHorizontalRadius)
                : -1;
        int lavaVertical = (dueMask & LAVA_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampVerticalRadius(local.lavaAnalyzerVerticalRadius)
                : -1;
        int hiddenHorizontal = (dueMask & HIDDEN_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(local.hiddenAnalyzerHorizontalRadius)
                : -1;
        int hiddenVertical = (dueMask & HIDDEN_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampVerticalRadius(local.hiddenAnalyzerVerticalRadius)
                : -1;
        int horizontalRadius = Math.max(lavaHorizontal, hiddenHorizontal);
        int verticalRadius = Math.max(lavaVertical, hiddenVertical);
        int lavaLimit = (dueMask & LAVA_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampOverlayResults(local.lavaAnalyzerMaxOverlayResults)
                : 0;
        int hiddenLimit = (dueMask & HIDDEN_MASK) != 0
                ? WorksiteVisibilityBudgetPolicy.clampOverlayResults(local.hiddenAnalyzerMaxOverlayResults)
                : 0;

        BlockPos origin = client.player.blockPosition();
        Vec3 eye = client.player.getEyePosition();
        int minX = origin.getX() - horizontalRadius;
        int maxX = origin.getX() + horizontalRadius;
        int minZ = origin.getZ() - horizontalRadius;
        int maxZ = origin.getZ() + horizontalRadius;
        if (!loadedChunks.load(client.level, minX, maxX, minZ, maxZ)) {
            return clearDueTargets(dueMask);
        }

        if ((dueMask & LAVA_MASK) != 0) lavaNearest.clear();
        if ((dueMask & HIDDEN_MASK) != 0) hiddenNearest.clear();
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();

        for (int z = minZ; z <= maxZ; z++) {
            int dzBlocks = Math.abs(z - originZ);
            for (int x = minX; x <= maxX; x++) {
                LevelChunk sourceChunk = loadedChunks.atBlock(x, z);
                if (sourceChunk == null) continue;
                int dxBlocks = Math.abs(x - originX);
                boolean lavaColumn = (dueMask & LAVA_MASK) != 0
                        && dxBlocks <= lavaHorizontal
                        && dzBlocks <= lavaHorizontal;
                boolean hiddenColumn = (dueMask & HIDDEN_MASK) != 0
                        && dxBlocks <= hiddenHorizontal
                        && dzBlocks <= hiddenHorizontal;
                if (!lavaColumn && !hiddenColumn) continue;

                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                    boolean lavaCandidate = lavaColumn && Math.abs(yOffset) <= lavaVertical;
                    boolean hiddenCandidate = hiddenColumn && Math.abs(yOffset) <= hiddenVertical;
                    if (!lavaCandidate && !hiddenCandidate) continue;

                    int y = originY + yOffset;
                    cursor.set(x, y, z);
                    double distanceSquared = Double.NaN;
                    BlockState state = hiddenCandidate ? sourceChunk.getBlockState(cursor) : null;

                    if (lavaCandidate) {
                        FluidState fluidState = state == null
                                ? sourceChunk.getFluidState(cursor)
                                : state.getFluidState();
                        boolean source = isSourceLava(fluidState);
                        boolean boundary = source && hasKnownSourceBoundary(client, sourceChunk, cursor);
                        if (LavaVisionPalettePolicy.shouldHighlight(true, source, boundary)) {
                            distanceSquared = distanceSquared(x, y, z, eye);
                            lavaNearest.offer(BlockPos.asLong(x, y, z), distanceSquared, lavaLimit);
                        }
                    }

                    if (hiddenCandidate) {
                        net.minecraft.world.level.block.Block block = state.getBlock();
                        if (!BuilderFocusVisibility.shouldHide(block)) {
                            int targetMask = hiddenTargetMask(block);
                            if (targetMask != 0 && (local.visualTargetMask & targetMask) != 0) {
                                if (Double.isNaN(distanceSquared)) {
                                    distanceSquared = distanceSquared(x, y, z, eye);
                                }
                                hiddenNearest.offer(
                                        BlockPos.asLong(x, y, z),
                                        distanceSquared,
                                        hiddenLimit);
                            }
                        }
                    }
                }
            }
        }

        int changedMask = 0;
        if ((dueMask & LAVA_MASK) != 0) {
            lavaNearest.sortPositions();
            if (lavaTargets.publish(
                    lavaNearest.positions(), lavaNearest.count(), eye.x, eye.y, eye.z)) {
                changedMask |= LAVA_MASK;
            }
        }
        if ((dueMask & HIDDEN_MASK) != 0) {
            hiddenNearest.sortPositions();
            if (hiddenTargets.publish(
                    hiddenNearest.positions(), hiddenNearest.count(), eye.x, eye.y, eye.z)) {
                changedMask |= HIDDEN_MASK;
            }
        }
        return changedMask;
    }

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
                neighborChunk = loadedChunks.atBlock(neighborCursor.getX(), neighborCursor.getZ());
                if (neighborChunk == null) {
                    neighborChunk = client.level.getChunkSource().getChunkNow(
                            neighborChunkX, neighborChunkZ);
                }
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

    private int hiddenTargetMask(net.minecraft.world.level.block.Block block) {
        Integer cached = hiddenTargetMasks.get(block);
        if (cached != null) return cached;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        VisualTargetSelectionPolicy.Target target =
                VisualTargetSelectionPolicy.hiddenTargetForBlockId(id == null ? "" : id.toString());
        int targetMask = target == null ? 0 : target.bitMask();
        hiddenTargetMasks.put(block, targetMask);
        return targetMask;
    }

    private int updateDueState(int index, int mask, int baseInterval, int fingerprint) {
        if (fingerprint != lastScanFingerprint[index]) {
            ticksUntilScan[index] = 0;
            stableScanCount[index] = 0;
        }
        if (ticksUntilScan[index] > 0) {
            ticksUntilScan[index]--;
            return 0;
        }
        return mask;
    }

    private void finishScan(int index, int baseInterval, boolean changed, int fingerprint) {
        if (changed || movementSinceLastScan[index]) {
            stableScanCount[index] = 0;
        } else {
            stableScanCount[index] = Math.min(stableScanCount[index] + 1, MAX_STABLE_BACKOFF_SHIFT);
        }
        ticksUntilScan[index] = Math.min(
                WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
                baseInterval * (1 << stableScanCount[index])) - 1;
        movementSinceLastScan[index] = false;
        lastScanFingerprint[index] = fingerprint;
    }

    private void observeMovement(int enabledMask, int lavaInterval, int hiddenInterval) {
        if ((enabledMask & LAVA_MASK) != 0) {
            movementSinceLastScan[LAVA_INDEX] = true;
            stableScanCount[LAVA_INDEX] = 0;
            ticksUntilScan[LAVA_INDEX] = Math.min(ticksUntilScan[LAVA_INDEX], lavaInterval - 1);
        }
        if ((enabledMask & HIDDEN_MASK) != 0) {
            movementSinceLastScan[HIDDEN_INDEX] = true;
            stableScanCount[HIDDEN_INDEX] = 0;
            ticksUntilScan[HIDDEN_INDEX] = Math.min(ticksUntilScan[HIDDEN_INDEX], hiddenInterval - 1);
        }
    }

    private void syncEnabledMask(int enabledMask) {
        int disabled = lastEnabledMask & ~enabledMask;
        int enabled = enabledMask & ~lastEnabledMask;
        if ((disabled & LAVA_MASK) != 0) clearMode(LAVA_INDEX, LAVA_MASK);
        if ((disabled & HIDDEN_MASK) != 0) clearMode(HIDDEN_INDEX, HIDDEN_MASK);
        if ((enabled & LAVA_MASK) != 0) {
            resetModeTiming(LAVA_INDEX);
            movementSinceLastScan[LAVA_INDEX] = true;
        }
        if ((enabled & HIDDEN_MASK) != 0) {
            resetModeTiming(HIDDEN_INDEX);
            movementSinceLastScan[HIDDEN_INDEX] = true;
        }
        lastEnabledMask = enabledMask;
    }

    private int clearDueTargets(int dueMask) {
        int changedMask = 0;
        if ((dueMask & LAVA_MASK) != 0) {
            lavaNearest.clear();
            if (lavaTargets.clear()) changedMask |= LAVA_MASK;
        }
        if ((dueMask & HIDDEN_MASK) != 0) {
            hiddenNearest.clear();
            if (hiddenTargets.clear()) changedMask |= HIDDEN_MASK;
        }
        return changedMask;
    }

    private void clearMode(int index, int mask) {
        if (mask == LAVA_MASK) {
            lavaTargets.clear();
            lavaNearest.clear();
        } else {
            hiddenTargets.clear();
            hiddenNearest.clear();
        }
        resetModeTiming(index);
    }

    private void resetModeTiming(int index) {
        ticksUntilScan[index] = 0;
        stableScanCount[index] = 0;
        lastScanFingerprint[index] = Integer.MIN_VALUE;
        movementSinceLastScan[index] = false;
    }

    private void render(LevelRenderContext context) {
        if (runtimeQuarantined || lastLevel == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || client.level != lastLevel
                || client.screen != null) return;

        int enabledMask = enabledMask();
        if ((enabledMask & LAVA_MASK) != 0
                && !lavaTargets.isEmpty()
                && !BuilderFocusVisibility.shouldHide(Blocks.LAVA)) {
            lavaRenderGuard.render(context);
        }
        if ((enabledMask & HIDDEN_MASK) != 0 && !hiddenTargets.isEmpty()) {
            hiddenRenderGuard.render(context);
        }
    }

    private int enabledMask() {
        int mask = 0;
        if (!lavaRenderGuard.isQuarantined() && FeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue()) {
            mask |= LAVA_MASK;
        }
        if (!hiddenRenderGuard.isQuarantined()
                && FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue()) {
            mask |= HIDDEN_MASK;
        }
        return mask;
    }

    private void resetScanState(ClientLevel level) {
        lavaTargets.clear();
        hiddenTargets.clear();
        lavaNearest.clear();
        hiddenNearest.clear();
        loadedChunks.clear();
        resetModeTiming(LAVA_INDEX);
        resetModeTiming(HIDDEN_INDEX);
        lastObservedPlayerBlock = Long.MIN_VALUE;
        lastEnabledMask = 0;
        lastLevel = level;
    }

    private static int lavaFingerprint(LocalFeatureConfig local) {
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

    private static int hiddenFingerprint(LocalFeatureConfig local) {
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

    private static double distanceSquared(int x, int y, int z, Vec3 eye) {
        double dx = x + 0.5 - eye.x;
        double dy = y + 0.5 - eye.y;
        double dz = z + 0.5 - eye.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void closeRenderers() {
        lavaRenderGuard.close();
        hiddenRenderGuard.close();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        runtimeQuarantined = true;
        resetScanState(null);
        closeRenderers();
    }

    @Override
    public void resetSession(Minecraft client) {
        resetScanState(null);
        lavaRenderGuard.resetSession();
        hiddenRenderGuard.resetSession();
    }
}
