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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;

/**
 * Bounded loaded-chunk visualization for occluded builder targets.
 *
 * <p>Lava source and hidden-material toggles keep independent target buffers and render guards,
 * but intentionally share one scan budget and one traversal schedule. No chunk is force-loaded.</p>
 */
public final class OccludedHighlightsFeature
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
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
                    "ChiseTweaks Occluded Blocks retained buffer",
                    "ChiseTweaks Occluded Blocks retained rendering"),
            hiddenTargets,
            FeatureDefinition.HIDDEN_SURFACE_TRACE.id(),
            "Occluded Blocks");
    private final NearestPositionBuffer lavaNearest = new NearestPositionBuffer(MAX_CANDIDATES);
    private final NearestPositionBuffer hiddenNearest = new NearestPositionBuffer(MAX_CANDIDATES);
    private final LoadedChunkWindow loadedChunks =
            new LoadedChunkWindow(WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES);
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();
    private final IdentityHashMap<Block, Integer> hiddenTargetMasks = new IdentityHashMap<>();

    private long lastObservedPlayerBlock = Long.MIN_VALUE;
    private int ticksUntilScan;
    private int stableScanCount;
    private int lastScanFingerprint = Integer.MIN_VALUE;
    private boolean movementSinceLastScan;
    private ClientLevel lastLevel;
    private int lastEnabledMask;
    private boolean runtimeQuarantined;

    @Override
    public String getId() {
        return "occluded_highlights";
    }

    @Override
    public void init() {
        populateHiddenTargetMasks();
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> closeRenderers());
        ChiseTweaksClient.LOGGER.info(
                "Occluded Highlights initialized with one bounded loaded-chunk traversal budget");
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

        int interval = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.occludedHighlightIntervalTicks);
        long currentPlayerBlock = client.player.blockPosition().asLong();
        if (currentPlayerBlock != lastObservedPlayerBlock) {
            lastObservedPlayerBlock = currentPlayerBlock;
            movementSinceLastScan = true;
            stableScanCount = 0;
            ticksUntilScan = Math.min(ticksUntilScan, interval - 1);
        }

        int fingerprint = scanFingerprint(local, enabledMask);
        if (fingerprint != lastScanFingerprint) {
            ticksUntilScan = 0;
            stableScanCount = 0;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }

        int changedMask = scanLoadedTargets(client, local, enabledMask);
        finishScan(interval, changedMask != 0, fingerprint);
    }

    private int scanLoadedTargets(Minecraft client, LocalFeatureConfig local, int enabledMask) {
        int horizontalRadius = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.occludedHighlightHorizontalRadius);
        int verticalRadius = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.occludedHighlightVerticalRadius);
        int limit = WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.occludedHighlightMaxOverlayResults);
        boolean scanLava = (enabledMask & LAVA_MASK) != 0;
        boolean scanHidden = (enabledMask & HIDDEN_MASK) != 0;

        BlockPos origin = client.player.blockPosition();
        Vec3 eye = client.player.getEyePosition();
        int minX = origin.getX() - horizontalRadius;
        int maxX = origin.getX() + horizontalRadius;
        int minZ = origin.getZ() - horizontalRadius;
        int maxZ = origin.getZ() + horizontalRadius;
        if (!loadedChunks.load(client.level, minX, maxX, minZ, maxZ)) {
            return clearEnabledTargets(enabledMask);
        }

        if (scanLava) lavaNearest.clear();
        if (scanHidden) hiddenNearest.clear();
        int originY = origin.getY();

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                LevelChunk sourceChunk = loadedChunks.atBlock(x, z);
                if (sourceChunk == null) continue;

                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                    int y = originY + yOffset;
                    cursor.set(x, y, z);
                    double distanceSquared = Double.NaN;
                    BlockState state = scanHidden ? sourceChunk.getBlockState(cursor) : null;

                    if (scanLava) {
                        FluidState fluidState = state == null
                                ? sourceChunk.getFluidState(cursor)
                                : state.getFluidState();
                        boolean source = isSourceLava(fluidState);
                        boolean boundary = source && hasKnownSourceBoundary(client, sourceChunk, cursor);
                        if (LavaVisionPalettePolicy.shouldHighlight(true, source, boundary)) {
                            distanceSquared = distanceSquared(x, y, z, eye);
                            lavaNearest.offer(BlockPos.asLong(x, y, z), distanceSquared, limit);
                        }
                    }

                    if (scanHidden) {
                        Block block = state.getBlock();
                        if (!BuilderFocusVisibility.shouldHide(block)) {
                            int targetMask = hiddenTargetMask(block);
                            if (targetMask != 0 && (local.visualTargetMask & targetMask) != 0) {
                                if (Double.isNaN(distanceSquared)) {
                                    distanceSquared = distanceSquared(x, y, z, eye);
                                }
                                hiddenNearest.offer(BlockPos.asLong(x, y, z), distanceSquared, limit);
                            }
                        }
                    }
                }
            }
        }

        int changedMask = 0;
        if (scanLava) {
            lavaNearest.sortPositions();
            if (lavaTargets.publish(
                    lavaNearest.positions(), lavaNearest.count(), eye.x, eye.y, eye.z)) {
                changedMask |= LAVA_MASK;
            }
        }
        if (scanHidden) {
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

    private void populateHiddenTargetMasks() {
        hiddenTargetMasks.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            VisualTargetSelectionPolicy.Target target =
                    VisualTargetSelectionPolicy.hiddenTargetForBlockId(id == null ? "" : id.toString());
            if (target != null) hiddenTargetMasks.put(block, target.bitMask());
        }
    }

    private int hiddenTargetMask(Block block) {
        return hiddenTargetMasks.getOrDefault(block, 0);
    }

    private void finishScan(int baseInterval, boolean changed, int fingerprint) {
        if (changed || movementSinceLastScan) {
            stableScanCount = 0;
        } else {
            stableScanCount = Math.min(stableScanCount + 1, MAX_STABLE_BACKOFF_SHIFT);
        }
        ticksUntilScan = Math.min(
                WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
                baseInterval * (1 << stableScanCount)) - 1;
        movementSinceLastScan = false;
        lastScanFingerprint = fingerprint;
    }

    private void syncEnabledMask(int enabledMask) {
        int disabled = lastEnabledMask & ~enabledMask;
        if ((disabled & LAVA_MASK) != 0) {
            lavaTargets.clear();
            lavaNearest.clear();
        }
        if ((disabled & HIDDEN_MASK) != 0) {
            hiddenTargets.clear();
            hiddenNearest.clear();
        }
        if (enabledMask != lastEnabledMask) {
            ticksUntilScan = 0;
            stableScanCount = 0;
            movementSinceLastScan = true;
            lastScanFingerprint = Integer.MIN_VALUE;
        }
        lastEnabledMask = enabledMask;
    }

    private int clearEnabledTargets(int enabledMask) {
        int changedMask = 0;
        if ((enabledMask & LAVA_MASK) != 0) {
            lavaNearest.clear();
            if (lavaTargets.clear()) changedMask |= LAVA_MASK;
        }
        if ((enabledMask & HIDDEN_MASK) != 0) {
            hiddenNearest.clear();
            if (hiddenTargets.clear()) changedMask |= HIDDEN_MASK;
        }
        return changedMask;
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
        ticksUntilScan = 0;
        stableScanCount = 0;
        lastScanFingerprint = Integer.MIN_VALUE;
        movementSinceLastScan = false;
        lastObservedPlayerBlock = Long.MIN_VALUE;
        lastEnabledMask = 0;
        lastLevel = level;
    }

    private static int scanFingerprint(LocalFeatureConfig local, int enabledMask) {
        int result = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                local.occludedHighlightHorizontalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                local.occludedHighlightVerticalRadius);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.occludedHighlightIntervalTicks);
        result = 31 * result + WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                local.occludedHighlightMaxOverlayResults);
        result = 31 * result + enabledMask;
        if ((enabledMask & HIDDEN_MASK) != 0) {
            result = 31 * result + local.visualTargetMask;
            result = 31 * result + local.hiddenSurfaceTraceColorPreset;
            result = 31 * result + local.hiddenSurfaceTraceOpacityPercent;
        }
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
