package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticDetail;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticEvent;
import dev.chise.chisetweaks.runtime.RuntimeDiagnostics;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
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
 * 古代の残骸アナライザーはクライアント専用・ネザー専用とし、すでにロード済みのクライアントチャンクだけを対象にする。
 */
public final class AncientDebrisAnalyzerFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final long[] EMPTY_POSITIONS = new long[0];

    private final ThroughWallMarkerRenderer renderer = new ThroughWallMarkerRenderer(
            ThroughWallMarkerRenderer.Style.ANCIENT_DEBRIS,
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS,
            "ChiseTweaks Ancient Debris Analyzer retained buffer",
            "ChiseTweaks Ancient Debris Analyzer retained rendering");
    private final ThroughWallPositionSnapshot visibleMarkers = new ThroughWallPositionSnapshot(
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS);
    private final NearestPositionBuffer nearestMarkers = new NearestPositionBuffer(
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS);
    private final Map<Long, long[]> positionsByChunk = new HashMap<>();
    private final long[] pendingBootstrapChunks = new long[
            AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT];
    private final long[] pendingValidationChunks = new long[
            AncientDebrisAnalyzerPolicy.MAX_TRACKED_CHUNKS];
    private final long[] chunkScanBuffer = new long[
            AncientDebrisAnalyzerPolicy.MAX_DEBRIS_PER_CHUNK];

    private ClientLevel lastLevel;
    private long lastPlayerBlock = Long.MIN_VALUE;
    private long lastPlayerChunk = Long.MIN_VALUE;
    private int lastRangeBlocks = Integer.MIN_VALUE;
    private int lastMaxMarkers = Integer.MIN_VALUE;
    private int ticksUntilValidation;
    private int pendingBootstrapIndex;
    private int pendingBootstrapCount;
    private int pendingValidationIndex;
    private int pendingValidationCount;
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
                "Ancient Debris Analyzer initialized with bounded loaded-chunk discovery, cache refresh and retained through-terrain rendering");
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            resetState(null);
            return;
        }
        if (isSessionQuarantined()) return;
        if (client == null || client.player == null || client.level == null || !isNether(client.level)) {
            resetState(null);
            return;
        }

        if (lastLevel != client.level) {
            resetState(client.level);
        }

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int rangeBlocks = AncientDebrisAnalyzerPolicy.clampRangeBlocks(local.ancientDebrisAnalyzerRangeBlocks);
        int maxMarkers = AncientDebrisAnalyzerPolicy.clampMaxMarkers(local.ancientDebrisAnalyzerMaxMarkers);
        BlockPos playerPosition = client.player.blockPosition();
        long playerBlock = playerPosition.asLong();
        int centerChunkX = playerPosition.getX() >> 4;
        int centerChunkZ = playerPosition.getZ() >> 4;
        long playerChunk = packChunk(centerChunkX, centerChunkZ);
        boolean rangeChanged = rangeBlocks != lastRangeBlocks;
        boolean playerChunkChanged = playerChunk != lastPlayerChunk;
        if (rangeChanged || playerChunkChanged) {
            scheduleLoadedChunkBootstrap(rangeBlocks, centerChunkX, centerChunkZ);
            clearPendingValidation();
            lastPlayerChunk = playerChunk;
            ticksUntilValidation = 0;
        }
        if (playerBlock != lastPlayerBlock || rangeChanged || maxMarkers != lastMaxMarkers) {
            lastPlayerBlock = playerBlock;
            lastRangeBlocks = rangeBlocks;
            lastMaxMarkers = maxMarkers;
            selectionDirty = true;
        }

        processPendingLoadedChunks(client);
        scheduleValidationWhenDue();
        processPendingValidation(client);

        if (selectionDirty) {
            publishVisibleMarkers(client, rangeBlocks, maxMarkers);
            selectionDirty = false;
        }
    }

    private void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled()
                || !isNether(level)
                || chunk == null
                || client.level != level
                || client.player == null) return;

        BlockPos playerPosition = client.player.blockPosition();
        int rangeBlocks = AncientDebrisAnalyzerPolicy.clampRangeBlocks(
                LocalFeatureConfig.getInstance().ancientDebrisAnalyzerRangeBlocks);
        if (!AncientDebrisAnalyzerPolicy.isChunkRelevant(
                playerPosition.getX() >> 4,
                playerPosition.getZ() >> 4,
                chunk.getPos().getMinBlockX() >> 4,
                chunk.getPos().getMinBlockZ() >> 4,
                rangeBlocks)) return;
        scanChunk(level, chunk);
    }

    private void onChunkUnload(ClientLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != level || (lastLevel != null && level != lastLevel)) return;
        if (positionsByChunk.remove(chunkKey(chunk)) != null) selectionDirty = true;
    }

    /**
     * ロード済みチャンク候補を固定上限queueへ積み、探索処理を複数のclient tickへ分散する。
     * 大きな検出範囲でも1 tickへ処理が集中しないよう、lookupとscanの実行数をtick単位で制限する。
     */
    private void scheduleLoadedChunkBootstrap(
            int rangeBlocks,
            int centerChunkX,
            int centerChunkZ) {
        int chunkRadius = AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(rangeBlocks);
        pruneTrackedChunksOutsideNeighborhood(centerChunkX, centerChunkZ, rangeBlocks);
        clearPendingBootstrap();
        for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
            for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
                long key = packChunk(chunkX, chunkZ);
                if (positionsByChunk.containsKey(key)) continue;
                if (pendingBootstrapCount >= pendingBootstrapChunks.length) return;
                pendingBootstrapChunks[pendingBootstrapCount++] = key;
            }
        }
    }

    private void processPendingLoadedChunks(Minecraft client) {
        if (client == null || client.level == null) {
            clearPendingBootstrap();
            return;
        }
        int processed = 0;
        while (pendingBootstrapIndex < pendingBootstrapCount
                && processed < AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK) {
            long key = pendingBootstrapChunks[pendingBootstrapIndex++];
            if (!positionsByChunk.containsKey(key)) {
                LevelChunk chunk = client.level.getChunkSource().getChunkNow(
                        unpackChunkX(key),
                        unpackChunkZ(key));
                if (chunk != null) scanChunk(client.level, chunk);
            }
            processed++;
        }
        if (pendingBootstrapIndex >= pendingBootstrapCount) clearPendingBootstrap();
    }

    private void scheduleValidationWhenDue() {
        if (hasPendingValidation()) return;
        if (ticksUntilValidation > 0) {
            ticksUntilValidation--;
            return;
        }
        pendingValidationIndex = 0;
        pendingValidationCount = 0;
        for (long key : positionsByChunk.keySet()) {
            if (pendingValidationCount >= pendingValidationChunks.length) break;
            pendingValidationChunks[pendingValidationCount++] = key;
        }
        ticksUntilValidation = Math.max(0, AncientDebrisAnalyzerPolicy.VALIDATION_INTERVAL_TICKS - 1);
    }

    /**
     * 追跡済みチャンクも定期的に再走査する。既存マーカーの消滅だけでなく、ロード後に配置された
     * Ancient Debrisも検出できるようにしつつ、1 tickの再走査数は固定上限へ抑える。
     */
    private void processPendingValidation(Minecraft client) {
        if (!hasPendingValidation() || client == null || client.level == null) return;
        int processed = 0;
        while (pendingValidationIndex < pendingValidationCount
                && processed < AncientDebrisAnalyzerPolicy.MAX_VALIDATION_CHUNKS_PER_TICK) {
            long key = pendingValidationChunks[pendingValidationIndex++];
            long[] cached = positionsByChunk.get(key);
            if (cached != null) {
                LevelChunk loaded = client.level.getChunkSource().getChunkNow(
                        unpackChunkX(key),
                        unpackChunkZ(key));
                if (loaded == null) {
                    positionsByChunk.remove(key);
                    selectionDirty = true;
                } else {
                    refreshTrackedChunk(client.level, loaded, cached);
                }
            }
            processed++;
        }
        if (pendingValidationIndex >= pendingValidationCount) clearPendingValidation();
    }

    private void pruneTrackedChunksOutsideNeighborhood(
            int centerChunkX,
            int centerChunkZ,
            int rangeBlocks) {
        boolean removed = false;
        Iterator<Long> iterator = positionsByChunk.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.next();
            if (AncientDebrisAnalyzerPolicy.isChunkRelevant(
                    centerChunkX,
                    centerChunkZ,
                    unpackChunkX(key),
                    unpackChunkZ(key),
                    rangeBlocks)) continue;
            iterator.remove();
            removed = true;
        }
        if (removed) selectionDirty = true;
    }

    private void scanChunk(ClientLevel level, LevelChunk chunk) {
        long key = chunkKey(chunk);
        if (positionsByChunk.containsKey(key)) return;
        if (positionsByChunk.size() >= AncientDebrisAnalyzerPolicy.MAX_TRACKED_CHUNKS) return;

        int count = scanChunkIntoBuffer(level, chunk);
        positionsByChunk.put(key, copyScanResult(count));
        if (count != 0) selectionDirty = true;
    }

    private void refreshTrackedChunk(ClientLevel level, LevelChunk chunk, long[] cached) {
        int count = scanChunkIntoBuffer(level, chunk);
        if (matchesScanResult(cached, count)) return;
        positionsByChunk.put(chunkKey(chunk), copyScanResult(count));
        selectionDirty = true;
    }

    private int scanChunkIntoBuffer(ClientLevel level, LevelChunk chunk) {
        int count = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || !section.maybeHas(state -> state.is(Blocks.ANCIENT_DEBRIS))) {
                continue;
            }
            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            int baseY = SectionPos.sectionToBlockCoord(sectionY);
            for (int y = 0; y < 16 && count < chunkScanBuffer.length; y++) {
                for (int z = 0; z < 16 && count < chunkScanBuffer.length; z++) {
                    for (int x = 0; x < 16 && count < chunkScanBuffer.length; x++) {
                        if (section.getBlockState(x, y, z).is(Blocks.ANCIENT_DEBRIS)) {
                            chunkScanBuffer[count++] = BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                        }
                    }
                }
            }
        }
        return count;
    }

    private boolean matchesScanResult(long[] cached, int count) {
        if (cached.length != count) return false;
        for (int index = 0; index < count; index++) {
            if (cached[index] != chunkScanBuffer[index]) return false;
        }
        return true;
    }

    private long[] copyScanResult(int count) {
        return count == 0 ? EMPTY_POSITIONS : Arrays.copyOf(chunkScanBuffer, count);
    }

    private void publishVisibleMarkers(Minecraft client, int rangeBlocks, int maxMarkers) {
        Vec3 eye = client.player.getEyePosition();
        nearestMarkers.clear();
        for (long[] chunkPositions : positionsByChunk.values()) {
            for (long packed : chunkPositions) {
                double dx = BlockPos.getX(packed) + 0.5 - eye.x;
                double dy = BlockPos.getY(packed) + 0.5 - eye.y;
                double dz = BlockPos.getZ(packed) + 0.5 - eye.z;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                if (!AncientDebrisAnalyzerPolicy.withinRangeSquared(distanceSquared, rangeBlocks)) continue;
                nearestMarkers.offer(packed, distanceSquared, maxMarkers);
            }
        }
        nearestMarkers.sortPositions();
        visibleMarkers.publish(
                nearestMarkers.positions(),
                nearestMarkers.count(),
                eye.x,
                eye.y,
                eye.z);
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled()
                || isSessionQuarantined()
                || visibleMarkers.isEmpty()
                || BuilderFocusVisibility.shouldHide(Blocks.ANCIENT_DEBRIS)) return;
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
            ChiseTweaksClient.LOGGER.error(
                    "Ancient Debris Analyzer rendering was quarantined after {}",
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.COMPONENT_QUARANTINE,
                    Minecraft.getInstance(),
                    RuntimeDiagnosticDetail.of("componentId", getId()),
                    RuntimeDiagnosticDetail.of("stage", "render"),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
        }
    }

    private void resetState(ClientLevel level) {
        positionsByChunk.clear();
        visibleMarkers.clear();
        nearestMarkers.clear();
        clearPendingBootstrap();
        clearPendingValidation();
        lastLevel = level;
        lastPlayerBlock = Long.MIN_VALUE;
        lastPlayerChunk = Long.MIN_VALUE;
        lastRangeBlocks = Integer.MIN_VALUE;
        lastMaxMarkers = Integer.MIN_VALUE;
        ticksUntilValidation = 0;
        selectionDirty = level != null;
    }

    private void clearPendingBootstrap() {
        pendingBootstrapIndex = 0;
        pendingBootstrapCount = 0;
    }

    private boolean hasPendingValidation() {
        return pendingValidationIndex < pendingValidationCount;
    }

    private void clearPendingValidation() {
        pendingValidationIndex = 0;
        pendingValidationCount = 0;
    }

    private static boolean isNether(ClientLevel level) {
        return level != null && Level.NETHER.equals(level.dimension());
    }

    private static long chunkKey(LevelChunk chunk) {
        return packChunk(chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4);
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static int unpackChunkX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackChunkZ(long packed) {
        return (int) packed;
    }

    private boolean isSessionQuarantined() {
        return runtimeQuarantined || renderQuarantined;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        runtimeQuarantined = true;
        resetState(null);
        try {
            renderer.close();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ancient Debris Analyzer renderer close failed after {}",
                    cleanupFailure.getClass().getSimpleName());
        }
    }

    @Override
    public void resetSession(Minecraft client) {
        resetState(null);
        renderQuarantined = false;
    }

    @Override
    public boolean isEnabled() {
        return FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)
                && !isSessionQuarantined()
                && LocalFeatureConfig.getInstance().ancientDebrisAnalyzerEnabled;
    }
}
