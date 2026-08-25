package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.WardenRiskAnalyzerPolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Clientが実際に保持しているloaded chunk / entityだけからWardenリスク情報を構築する。
 * server-side warning levelや未来のspawn座標は推測値としても公開しない。
 */
public final class WardenRiskAnalyzerFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final long[] EMPTY_POSITIONS = new long[0];
    private static final AnalysisSnapshot INACTIVE = new AnalysisSnapshot(false, 0, 0, 0, false, false);
    private static volatile AnalysisSnapshot currentSnapshot = INACTIVE;

    private final ThroughWallMarkerRenderer renderer = new ThroughWallMarkerRenderer(
            ThroughWallMarkerRenderer.Style.WARDEN_RISK,
            WardenRiskAnalyzerPolicy.MAX_MAX_MARKERS,
            "ChiseTweaks Warden Risk Analyzer retained buffer",
            "ChiseTweaks Warden Risk Analyzer retained rendering");
    private final ThroughWallPositionSnapshot visibleMarkers = new ThroughWallPositionSnapshot(
            WardenRiskAnalyzerPolicy.MAX_MAX_MARKERS);
    private final ThroughWallRenderGuard renderGuard = new ThroughWallRenderGuard(
            renderer,
            visibleMarkers,
            FeatureDefinition.WARDEN_RISK_ANALYZER.id(),
            "Warden Risk Analyzer");
    private final NearestPositionBuffer nearestMarkers = new NearestPositionBuffer(
            WardenRiskAnalyzerPolicy.MAX_MAX_MARKERS);
    private final Map<Long, ChunkRisk> riskByChunk = new HashMap<>();
    private final long[] pendingBootstrapChunks = new long[WardenRiskAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT];
    private final long[] pendingValidationChunks = new long[WardenRiskAnalyzerPolicy.MAX_TRACKED_CHUNKS];
    private final long[] dangerousPositionBuffer =
            new long[WardenRiskAnalyzerPolicy.MAX_DANGEROUS_SHRIEKERS_PER_CHUNK];

    private ClientLevel lastLevel;
    private long lastPlayerBlock = Long.MIN_VALUE;
    private long lastPlayerChunk = Long.MIN_VALUE;
    private int lastRangeBlocks = Integer.MIN_VALUE;
    private int lastMaxMarkers = Integer.MIN_VALUE;
    private int ticksUntilValidation;
    private int ticksUntilEntityCheck;
    private int pendingBootstrapIndex;
    private int pendingBootstrapCount;
    private int pendingValidationIndex;
    private int pendingValidationCount;
    private int scanDangerousTotal;
    private int scanOtherShriekers;
    private int scanSensors;
    private boolean loadedWardenDetected;
    private boolean selectionDirty;
    private boolean runtimeQuarantined;

    public record AnalysisSnapshot(
            boolean enabled,
            int dangerousShriekers,
            int otherShriekers,
            int sensors,
            boolean sensorsCapped,
            boolean loadedWardenDetected) {
    }

    public static AnalysisSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    @Override
    public String getId() {
        return FeatureDefinition.WARDEN_RISK_ANALYZER.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.WARDEN_RISK_ANALYZER.englishName();
    }

    @Override
    public void init() {
        ClientChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderGuard.close());
        ChiseTweaksClient.LOGGER.info(
                "Warden Risk Analyzer initialized with bounded loaded-chunk observation and confidence-safe output");
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            resetState(null);
            return;
        }
        if (isSessionQuarantined()) return;
        if (client == null || client.player == null || client.level == null) {
            resetState(null);
            return;
        }
        if (lastLevel != client.level) resetState(client.level);

        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        int rangeBlocks = WardenRiskAnalyzerPolicy.clampRangeBlocks(local.wardenRiskAnalyzerRangeBlocks);
        int maxMarkers = WardenRiskAnalyzerPolicy.clampMaxMarkers(local.wardenRiskAnalyzerMaxMarkers);
        BlockPos playerPosition = client.player.blockPosition();
        long playerBlock = playerPosition.asLong();
        long playerChunk = packChunk(playerPosition.getX() >> 4, playerPosition.getZ() >> 4);
        boolean rangeChanged = rangeBlocks != lastRangeBlocks;
        boolean playerChunkChanged = playerChunk != lastPlayerChunk;
        if (rangeChanged || playerChunkChanged) {
            scheduleLoadedChunkBootstrap(
                    rangeBlocks,
                    playerPosition.getX() >> 4,
                    playerPosition.getZ() >> 4);
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
        checkLoadedWardenWhenDue(client);

        if (selectionDirty) {
            publishVisibleMarkersAndSnapshot(client, rangeBlocks, maxMarkers);
            selectionDirty = false;
        }
    }

    private void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || chunk == null || client.level != level || client.player == null) return;
        BlockPos playerPosition = client.player.blockPosition();
        int rangeBlocks = WardenRiskAnalyzerPolicy.clampRangeBlocks(
                LocalFeatureConfig.getInstance().wardenRiskAnalyzerRangeBlocks);
        if (!WardenRiskAnalyzerPolicy.isChunkRelevant(
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
        if (riskByChunk.remove(chunkKey(chunk)) != null) selectionDirty = true;
    }

    private void scheduleLoadedChunkBootstrap(int rangeBlocks, int centerChunkX, int centerChunkZ) {
        int chunkRadius = WardenRiskAnalyzerPolicy.chunkRadiusForRangeBlocks(rangeBlocks);
        pruneTrackedChunksOutsideNeighborhood(centerChunkX, centerChunkZ, rangeBlocks);
        clearPendingBootstrap();
        for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
            for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
                long key = packChunk(chunkX, chunkZ);
                if (riskByChunk.containsKey(key)) continue;
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
                && processed < WardenRiskAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK) {
            long key = pendingBootstrapChunks[pendingBootstrapIndex++];
            if (!riskByChunk.containsKey(key)) {
                LevelChunk chunk = client.level.getChunkSource().getChunkNow(unpackChunkX(key), unpackChunkZ(key));
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
        for (long key : riskByChunk.keySet()) {
            if (pendingValidationCount >= pendingValidationChunks.length) break;
            pendingValidationChunks[pendingValidationCount++] = key;
        }
        ticksUntilValidation = Math.max(0, WardenRiskAnalyzerPolicy.VALIDATION_INTERVAL_TICKS - 1);
    }

    private void processPendingValidation(Minecraft client) {
        if (!hasPendingValidation() || client == null || client.level == null) return;
        int processed = 0;
        while (pendingValidationIndex < pendingValidationCount
                && processed < WardenRiskAnalyzerPolicy.MAX_VALIDATION_CHUNKS_PER_TICK) {
            long key = pendingValidationChunks[pendingValidationIndex++];
            ChunkRisk cached = riskByChunk.get(key);
            if (cached != null) {
                LevelChunk loaded = client.level.getChunkSource().getChunkNow(unpackChunkX(key), unpackChunkZ(key));
                if (loaded == null) {
                    riskByChunk.remove(key);
                    selectionDirty = true;
                } else {
                    refreshTrackedChunk(client.level, loaded, cached);
                }
            }
            processed++;
        }
        if (pendingValidationIndex >= pendingValidationCount) clearPendingValidation();
    }

    private void checkLoadedWardenWhenDue(Minecraft client) {
        if (ticksUntilEntityCheck > 0) {
            ticksUntilEntityCheck--;
            return;
        }
        boolean detected = false;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Warden) {
                detected = true;
                break;
            }
        }
        if (detected != loadedWardenDetected) {
            loadedWardenDetected = detected;
            selectionDirty = true;
        }
        ticksUntilEntityCheck = Math.max(0, WardenRiskAnalyzerPolicy.ENTITY_CHECK_INTERVAL_TICKS - 1);
    }

    private void pruneTrackedChunksOutsideNeighborhood(
            int centerChunkX,
            int centerChunkZ,
            int rangeBlocks) {
        boolean removed = false;
        Iterator<Long> iterator = riskByChunk.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.next();
            if (WardenRiskAnalyzerPolicy.isChunkRelevant(
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
        if (riskByChunk.containsKey(key)) return;
        if (riskByChunk.size() >= WardenRiskAnalyzerPolicy.MAX_TRACKED_CHUNKS) return;
        scanChunkIntoBuffer(level, chunk);
        riskByChunk.put(key, copyScanResult());
        selectionDirty = true;
    }

    private void refreshTrackedChunk(ClientLevel level, LevelChunk chunk, ChunkRisk cached) {
        scanChunkIntoBuffer(level, chunk);
        if (cached.matches(
                dangerousPositionBuffer,
                Math.min(scanDangerousTotal, dangerousPositionBuffer.length),
                scanDangerousTotal,
                scanOtherShriekers,
                scanSensors)) return;
        riskByChunk.put(chunkKey(chunk), copyScanResult());
        selectionDirty = true;
    }

    private void scanChunkIntoBuffer(ClientLevel level, LevelChunk chunk) {
        scanDangerousTotal = 0;
        scanOtherShriekers = 0;
        scanSensors = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || !section.maybeHas(WardenRiskAnalyzerFeature::isRelevantSculkState)) continue;
            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            int baseY = SectionPos.sectionToBlockCoord(sectionY);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(Blocks.SCULK_SHRIEKER)) {
                            if (canSummon(state)) {
                                if (scanDangerousTotal < dangerousPositionBuffer.length) {
                                    dangerousPositionBuffer[scanDangerousTotal] =
                                            BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                                }
                                scanDangerousTotal++;
                            } else {
                                scanOtherShriekers++;
                            }
                        } else if (isSensor(state)) {
                            scanSensors++;
                        }
                    }
                }
            }
        }
    }

    private ChunkRisk copyScanResult() {
        int retainedCount = Math.min(scanDangerousTotal, dangerousPositionBuffer.length);
        long[] retained = retainedCount == 0
                ? EMPTY_POSITIONS
                : Arrays.copyOf(dangerousPositionBuffer, retainedCount);
        return new ChunkRisk(retained, scanDangerousTotal, scanOtherShriekers, scanSensors);
    }

    private void publishVisibleMarkersAndSnapshot(Minecraft client, int rangeBlocks, int maxMarkers) {
        Vec3 eye = client.player.getEyePosition();
        nearestMarkers.clear();
        int dangerousTotal = 0;
        int otherTotal = 0;
        int sensorTotal = 0;
        for (ChunkRisk risk : riskByChunk.values()) {
            dangerousTotal += risk.dangerousTotal;
            otherTotal += risk.otherShriekers;
            sensorTotal += risk.sensors;
            for (long packed : risk.dangerousPositions) {
                double dx = BlockPos.getX(packed) + 0.5 - eye.x;
                double dy = BlockPos.getY(packed) + 0.5 - eye.y;
                double dz = BlockPos.getZ(packed) + 0.5 - eye.z;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                if (!WardenRiskAnalyzerPolicy.withinRangeSquared(distanceSquared, rangeBlocks)) continue;
                nearestMarkers.offer(packed, distanceSquared, maxMarkers);
            }
        }
        nearestMarkers.sortPositions();
        visibleMarkers.publish(
                nearestMarkers.positions(), nearestMarkers.count(), eye.x, eye.y, eye.z);
        boolean sensorsCapped = sensorTotal > WardenRiskAnalyzerPolicy.MAX_SENSOR_RESULTS;
        currentSnapshot = new AnalysisSnapshot(
                true,
                dangerousTotal,
                otherTotal,
                Math.min(sensorTotal, WardenRiskAnalyzerPolicy.MAX_SENSOR_RESULTS),
                sensorsCapped,
                loadedWardenDetected);
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled()
                || visibleMarkers.isEmpty()
                || BuilderFocusVisibility.shouldHide(Blocks.SCULK_SHRIEKER)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.level != lastLevel || client.screen != null) return;
        renderGuard.render(context);
    }

    private void resetState(ClientLevel level) {
        riskByChunk.clear();
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
        ticksUntilEntityCheck = 0;
        loadedWardenDetected = false;
        selectionDirty = level != null;
        currentSnapshot = level == null ? INACTIVE : new AnalysisSnapshot(true, 0, 0, 0, false, false);
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

    private static boolean isRelevantSculkState(BlockState state) {
        return state.is(Blocks.SCULK_SHRIEKER) || isSensor(state);
    }

    private static boolean canSummon(BlockState state) {
        return state.hasProperty(BlockStateProperties.CAN_SUMMON)
                && state.getValue(BlockStateProperties.CAN_SUMMON);
    }

    private static boolean isSensor(BlockState state) {
        return state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.CALIBRATED_SCULK_SENSOR);
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
        return runtimeQuarantined || renderGuard.isQuarantined();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        runtimeQuarantined = true;
        resetState(null);
        renderGuard.close();
    }

    @Override
    public void resetSession(Minecraft client) {
        resetState(null);
        renderGuard.resetSession();
    }

    @Override
    public boolean isEnabled() {
        return FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.WARDEN_RISK_ANALYZER)
                && !isSessionQuarantined()
                && LocalFeatureConfig.getInstance().wardenRiskAnalyzerEnabled;
    }

    private static final class ChunkRisk {
        private final long[] dangerousPositions;
        private final int dangerousTotal;
        private final int otherShriekers;
        private final int sensors;

        private ChunkRisk(long[] dangerousPositions, int dangerousTotal, int otherShriekers, int sensors) {
            this.dangerousPositions = dangerousPositions;
            this.dangerousTotal = dangerousTotal;
            this.otherShriekers = otherShriekers;
            this.sensors = sensors;
        }

        private boolean matches(
                long[] candidatePositions,
                int candidateCount,
                int candidateDangerousTotal,
                int candidateOtherShriekers,
                int candidateSensors) {
            if (dangerousTotal != candidateDangerousTotal
                    || otherShriekers != candidateOtherShriekers
                    || sensors != candidateSensors
                    || dangerousPositions.length != candidateCount) return false;
            for (int index = 0; index < candidateCount; index++) {
                if (dangerousPositions[index] != candidatePositions[index]) return false;
            }
            return true;
        }
    }
}
