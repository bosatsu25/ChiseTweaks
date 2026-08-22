package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;
import dev.chise.chisetweaks.feature.SessionAwareFeature;
import dev.chise.chisetweaks.feature.TickingFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Nether-only, client-only analyzer for Ancient Debris already present in loaded client chunks.
 *
 * <p>Chunk contents are scanned once when they become relevant: on client chunk load, on one-shot
 * bootstrap when the feature is enabled, or when the player crosses a chunk boundary and an
 * already-loaded chunk enters the configured analyzer neighborhood. Steady-state ticks only filter
 * cached packed positions by player distance and occasionally validate already-known markers. No
 * unloaded chunk is requested or generated.</p>
 */
public final class AncientDebrisAnalyzerFeature implements TickingFeature, SessionAwareFeature {
    private static final long[] EMPTY_POSITIONS = new long[0];

    private final AncientDebrisThroughWallRenderer renderer = new AncientDebrisThroughWallRenderer();
    private final AncientDebrisSnapshot visibleMarkers = new AncientDebrisSnapshot(
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS);
    private final Map<Long, long[]> positionsByChunk = new HashMap<>();
    private final long[] selectedPositions = new long[AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS];
    private final double[] selectedDistanceSquared = new double[AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS];

    private ClientLevel lastLevel;
    private long lastPlayerBlock = Long.MIN_VALUE;
    private long lastPlayerChunk = Long.MIN_VALUE;
    private int lastRangeBlocks = Integer.MIN_VALUE;
    private int lastMaxMarkers = Integer.MIN_VALUE;
    private int ticksUntilValidation;
    private boolean selectionDirty;
    private boolean renderQuarantined;
    private boolean runtimeQuarantined;

    @Override
    public String getId() {
        return FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.englishName();
    }

    @Override
    public void init() {
        ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
        ChiseTweaksClient.LOGGER.info(
                "Ancient Debris Analyzer initialized with loaded-chunk discovery and retained through-terrain rendering");
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            clearSessionData();
            return;
        }
        if (isSessionQuarantined()) return;
        if (client == null || client.player == null || client.level == null || !isNether(client.level)) {
            clearSessionData();
            return;
        }

        if (lastLevel != client.level) {
            resetForLevel(client.level);
            bootstrapLoadedChunks(client);
        }

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int rangeBlocks = AncientDebrisAnalyzerPolicy.clampRangeBlocks(local.ancientDebrisAnalyzerRangeBlocks);
        int maxMarkers = AncientDebrisAnalyzerPolicy.clampMaxMarkers(local.ancientDebrisAnalyzerMaxMarkers);
        BlockPos playerPosition = client.player.blockPosition();
        long playerBlock = playerPosition.asLong();
        long playerChunk = packChunk(playerPosition.getX() >> 4, playerPosition.getZ() >> 4);
        boolean rangeChanged = rangeBlocks != lastRangeBlocks;
        boolean playerChunkChanged = playerChunk != lastPlayerChunk;
        if (rangeChanged || playerChunkChanged) {
            bootstrapLoadedChunks(client);
            lastPlayerChunk = playerChunk;
        }
        if (playerBlock != lastPlayerBlock || rangeChanged || maxMarkers != lastMaxMarkers) {
            lastPlayerBlock = playerBlock;
            lastRangeBlocks = rangeBlocks;
            lastMaxMarkers = maxMarkers;
            selectionDirty = true;
        }

        if (ticksUntilValidation <= 0) {
            validateCachedMarkers(client.level);
            ticksUntilValidation = AncientDebrisAnalyzerPolicy.VALIDATION_INTERVAL_TICKS;
        } else {
            ticksUntilValidation--;
        }

        if (selectionDirty) {
            publishVisibleMarkers(client, rangeBlocks, maxMarkers);
            selectionDirty = false;
        }
    }

    private void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        if (!isEnabled() || !isNether(level) || level != Minecraft.getInstance().level) return;
        scanChunk(level, chunk);
    }

    private void onChunkUnload(ClientLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) return;
        if (positionsByChunk.remove(chunkKey(chunk)) != null) selectionDirty = true;
    }

    private void bootstrapLoadedChunks(Minecraft client) {
        if (client == null || client.player == null || client.level == null || !isNether(client.level)) return;
        int rangeBlocks = AncientDebrisAnalyzerPolicy.clampRangeBlocks(
                LocalFeatureConfig.getInstance().ancientDebrisAnalyzerRangeBlocks);
        int chunkRadius = Math.min(16, (rangeBlocks + 15) / 16 + 1);
        int centerChunkX = client.player.blockPosition().getX() >> 4;
        int centerChunkZ = client.player.blockPosition().getZ() >> 4;
        for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
            for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
                LevelChunk chunk = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                long key = chunkKey(chunk);
                if (!positionsByChunk.containsKey(key)) scanChunk(client.level, chunk);
            }
        }
    }

    private void scanChunk(ClientLevel level, LevelChunk chunk) {
        if (positionsByChunk.size() >= AncientDebrisAnalyzerPolicy.MAX_TRACKED_CHUNKS) return;
        long key = chunkKey(chunk);
        if (positionsByChunk.containsKey(key)) return;

        long[] found = new long[AncientDebrisAnalyzerPolicy.MAX_DEBRIS_PER_CHUNK];
        int count = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty() || !section.maybeHas(state -> state.is(Blocks.ANCIENT_DEBRIS))) {
                continue;
            }
            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            int baseY = SectionPos.sectionToBlockCoord(sectionY);
            for (int y = 0; y < 16 && count < found.length; y++) {
                for (int z = 0; z < 16 && count < found.length; z++) {
                    for (int x = 0; x < 16 && count < found.length; x++) {
                        if (section.getBlockState(x, y, z).is(Blocks.ANCIENT_DEBRIS)) {
                            found[count++] = BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                        }
                    }
                }
            }
        }

        positionsByChunk.put(key, count == 0 ? EMPTY_POSITIONS : Arrays.copyOf(found, count));
        if (count != 0) selectionDirty = true;
    }

    private void validateCachedMarkers(ClientLevel level) {
        boolean changed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Iterator<Map.Entry<Long, long[]>> iterator = positionsByChunk.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, long[]> entry = iterator.next();
            long[] cached = entry.getValue();
            if (cached.length == 0) continue;
            int write = 0;
            for (long packed : cached) {
                cursor.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
                LevelChunk loaded = level.getChunkSource().getChunkNow(cursor.getX() >> 4, cursor.getZ() >> 4);
                if (loaded != null && loaded.getBlockState(cursor).is(Blocks.ANCIENT_DEBRIS)) {
                    cached[write++] = packed;
                } else {
                    changed = true;
                }
            }
            if (write != cached.length) entry.setValue(write == 0 ? EMPTY_POSITIONS : Arrays.copyOf(cached, write));
        }
        if (changed) selectionDirty = true;
    }

    private void publishVisibleMarkers(Minecraft client, int rangeBlocks, int maxMarkers) {
        Vec3 eye = client.player.getEyePosition();
        int count = 0;
        for (long[] chunkPositions : positionsByChunk.values()) {
            for (long packed : chunkPositions) {
                double dx = BlockPos.getX(packed) + 0.5 - eye.x;
                double dy = BlockPos.getY(packed) + 0.5 - eye.y;
                double dz = BlockPos.getZ(packed) + 0.5 - eye.z;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                if (!AncientDebrisAnalyzerPolicy.withinRangeSquared(distanceSquared, rangeBlocks)) continue;
                count = retainNearest(packed, distanceSquared, count, maxMarkers);
            }
        }
        Arrays.sort(selectedPositions, 0, count);
        visibleMarkers.publish(selectedPositions, count, eye.x, eye.y, eye.z);
    }

    private int retainNearest(long packed, double distanceSquared, int count, int limit) {
        if (count < limit) {
            selectedPositions[count] = packed;
            selectedDistanceSquared[count] = distanceSquared;
            return count + 1;
        }
        int farthestIndex = 0;
        double farthestDistance = selectedDistanceSquared[0];
        for (int index = 1; index < count; index++) {
            if (selectedDistanceSquared[index] > farthestDistance) {
                farthestDistance = selectedDistanceSquared[index];
                farthestIndex = index;
            }
        }
        if (distanceSquared >= farthestDistance) return count;
        selectedPositions[farthestIndex] = packed;
        selectedDistanceSquared[farthestIndex] = distanceSquared;
        return count;
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled() || isSessionQuarantined() || visibleMarkers.isEmpty()) return;
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || client.level != lastLevel || client.screen != null) return;
            renderer.render(context, visibleMarkers);
        } catch (RuntimeException | LinkageError failure) {
            renderQuarantined = true;
            visibleMarkers.clear();
            try {
                renderer.resetAfterFailure();
            } catch (RuntimeException | LinkageError cleanupFailure) {
                ChiseTweaksClient.LOGGER.warn(
                        "Ancient Debris Analyzer renderer cleanup failed after {}",
                        cleanupFailure.getClass().getSimpleName());
            }
            disableAfterQuarantine();
            ChiseTweaksClient.LOGGER.error(
                    "Ancient Debris Analyzer rendering was quarantined after {}",
                    failure.getClass().getSimpleName());
        }
    }

    private void resetForLevel(ClientLevel level) {
        positionsByChunk.clear();
        visibleMarkers.clear();
        lastLevel = level;
        lastPlayerBlock = Long.MIN_VALUE;
        lastPlayerChunk = Long.MIN_VALUE;
        lastRangeBlocks = Integer.MIN_VALUE;
        lastMaxMarkers = Integer.MIN_VALUE;
        ticksUntilValidation = 0;
        selectionDirty = true;
    }

    private void clearSessionData() {
        positionsByChunk.clear();
        visibleMarkers.clear();
        lastLevel = null;
        lastPlayerBlock = Long.MIN_VALUE;
        lastPlayerChunk = Long.MIN_VALUE;
        lastRangeBlocks = Integer.MIN_VALUE;
        lastMaxMarkers = Integer.MIN_VALUE;
        ticksUntilValidation = 0;
        selectionDirty = false;
    }

    private void disableAfterQuarantine() {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        if (!local.ancientDebrisAnalyzerEnabled) return;
        local.ancientDebrisAnalyzerEnabled = false;
        local.save();
    }

    private static boolean isNether(ClientLevel level) {
        return level != null && Level.NETHER.equals(level.dimension());
    }

    private static long chunkKey(LevelChunk chunk) {
        return chunk.getPos().toLong();
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private boolean isSessionQuarantined() {
        return runtimeQuarantined || renderQuarantined;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        runtimeQuarantined = true;
        clearSessionData();
        try {
            renderer.close();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ancient Debris Analyzer renderer close failed after {}",
                    cleanupFailure.getClass().getSimpleName());
        }
        disableAfterQuarantine();
    }

    @Override
    public void resetSession(Minecraft client) {
        clearSessionData();
        renderQuarantined = false;
    }

    @Override
    public boolean isEnabled() {
        return PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)
                && !isSessionQuarantined()
                && LocalFeatureConfig.getInstance().ancientDebrisAnalyzerEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        boolean effective = PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)
                && enabled
                && !isSessionQuarantined();
        local.ancientDebrisAnalyzerEnabled = effective;
        local.save();
        clearSessionData();
        if (effective) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.player != null && isNether(client.level)) {
                resetForLevel(client.level);
                bootstrapLoadedChunks(client);
            }
        }
    }
}
