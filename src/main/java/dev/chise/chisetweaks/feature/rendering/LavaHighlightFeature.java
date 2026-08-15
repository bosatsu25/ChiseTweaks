package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.feature.SessionAwareFeature;
import dev.chise.chisetweaks.feature.TickingFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded, client-only analyzer for nearby lava source blocks.
 *
 * <p>The feature never recolors lava, loads chunks, mutates the world, or sends packets. It scans
 * only already-loaded nearby blocks on the existing Chise tick cadence, retains a bounded set of
 * exposed lava sources, and renders a full-bright 1x1x1 source wireframe through nearby terrain.
 * The wireframe stays in Chise's reserved deep-green family and strengthens smoothly toward
 * {@code #075B32} as the player approaches the source.</p>
 */
public class LavaHighlightFeature implements TickingFeature, SessionAwareFeature {
    private static final int MAX_CANDIDATES = WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final LavaHighlightConfig config = new LavaHighlightConfig();
    private final LavaAnalyzerThroughWallRenderer analyzerRenderer = new LavaAnalyzerThroughWallRenderer();
    private final int[] candidateX = new int[MAX_CANDIDATES];
    private final int[] candidateY = new int[MAX_CANDIDATES];
    private final int[] candidateZ = new int[MAX_CANDIDATES];
    private final double[] candidateDistanceSquared = new double[MAX_CANDIDATES];
    private final boolean[] loadedChunkBuffer = new boolean[
            WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();

    private volatile List<BlockPos> analyzedSources = List.of();
    private int ticksUntilScan;
    private boolean renderQuarantined;

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
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> analyzerRenderer.close());
        ChiseTweaksClient.LOGGER.info(
                "Lava Analyzer initialized with bounded through-terrain proximity rendering");
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            clearTargets();
            ticksUntilScan = 0;
            return;
        }
        if (renderQuarantined) return;
        if (client == null || client.player == null || client.level == null) {
            clearTargets();
            ticksUntilScan = 0;
            return;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        ticksUntilScan = WorksiteVisibilityBudgetPolicy.clampIntervalTicks(
                local.lavaAnalyzerIntervalTicks) - 1;
        scanLoadedSources(client, local);
    }

    private void scanLoadedSources(Minecraft client, LocalFeatureConfig local) {
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
            clearTargets();
            return;
        }

        int chunkIndex = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                loadedChunkBuffer[chunkIndex++] = client.level.getChunkSource().hasChunk(chunkX, chunkZ);
            }
        }

        int count = 0;
        int originY = origin.getY();
        for (int z = minZ; z <= maxZ; z++) {
            int loadedRow = ((z >> 4) - minChunkZ) * chunkSpanX;
            for (int x = minX; x <= maxX; x++) {
                int loadedIndex = loadedRow + ((x >> 4) - minChunkX);
                if (!loadedChunkBuffer[loadedIndex]) continue;
                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                    int y = originY + yOffset;
                    cursor.set(x, y, z);
                    FluidState fluidState = client.level.getFluidState(cursor);
                    boolean source = isSourceLava(fluidState);
                    boolean exposed = source && isExposedSource(client, cursor);
                    if (!LavaVisionPalettePolicy.shouldHighlight(true, source, exposed)) continue;

                    double dx = x + 0.5 - eye.x;
                    double dy = y + 0.5 - eye.y;
                    double dz = z + 0.5 - eye.z;
                    count = retainNearest(x, y, z, dx * dx + dy * dy + dz * dz, count, limit);
                }
            }
        }

        if (count == 0) {
            clearTargets();
            return;
        }
        ArrayList<BlockPos> prepared = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            prepared.add(new BlockPos(candidateX[index], candidateY[index], candidateZ[index]));
        }
        analyzedSources = List.copyOf(prepared);
    }

    private boolean isExposedSource(Minecraft client, BlockPos position) {
        for (Direction direction : DIRECTIONS) {
            neighborCursor.set(
                    position.getX() + direction.getStepX(),
                    position.getY() + direction.getStepY(),
                    position.getZ() + direction.getStepZ());
            if (!isSourceLava(client.level.getFluidState(neighborCursor))) return true;
        }
        return false;
    }

    private static boolean isSourceLava(FluidState fluidState) {
        if (fluidState == null || !fluidState.isSource()) return false;
        return fluidState.getType() == Fluids.LAVA || fluidState.getType() == Fluids.FLOWING_LAVA;
    }

    private int retainNearest(int x, int y, int z, double distanceSquared, int count, int limit) {
        if (count < limit) {
            candidateX[count] = x;
            candidateY[count] = y;
            candidateZ[count] = z;
            candidateDistanceSquared[count] = distanceSquared;
            return count + 1;
        }

        int farthestIndex = 0;
        double farthestDistance = candidateDistanceSquared[0];
        for (int index = 1; index < count; index++) {
            if (candidateDistanceSquared[index] > farthestDistance) {
                farthestDistance = candidateDistanceSquared[index];
                farthestIndex = index;
            }
        }
        if (distanceSquared >= farthestDistance) return count;

        candidateX[farthestIndex] = x;
        candidateY[farthestIndex] = y;
        candidateZ[farthestIndex] = z;
        candidateDistanceSquared[farthestIndex] = distanceSquared;
        return count;
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled() || renderQuarantined) return;
        List<BlockPos> snapshot = analyzedSources;
        if (snapshot.isEmpty()) return;
        try {
            renderSafely(context, snapshot);
        } catch (RuntimeException | LinkageError failure) {
            renderQuarantined = true;
            clearTargets();
            ChiseTweaksClient.LOGGER.error(
                    "Lava Analyzer rendering was quarantined after {}",
                    failure.getClass().getSimpleName());
        }
    }

    private void renderSafely(LevelRenderContext context, List<BlockPos> snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) return;
        analyzerRenderer.render(context, snapshot);
    }

    private void clearTargets() {
        if (!analyzedSources.isEmpty()) analyzedSources = List.of();
    }

    @Override
    public void resetSession(Minecraft client) {
        clearTargets();
        ticksUntilScan = 0;
        renderQuarantined = false;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) clearTargets();
    }

    public LavaHighlightConfig getConfig() {
        return config;
    }
}
