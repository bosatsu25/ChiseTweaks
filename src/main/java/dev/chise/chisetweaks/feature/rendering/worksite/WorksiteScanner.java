package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteCandidateRetentionPolicy;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

 
final class WorksiteScanner {
    private static final double[][] SOLID_SAMPLES = {{0.50, 0.50, 0.50}};
    private static final double[][] THIN_TECHNICAL_SAMPLES = {
            {0.50, 0.08, 0.50},
            {0.25, 0.08, 0.50},
            {0.75, 0.08, 0.50},
            {0.50, 0.08, 0.25},
            {0.50, 0.08, 0.75},
            {0.50, 0.32, 0.50}
    };
    private static final double[][] SHAPED_BLOCK_SAMPLES = {
            {0.50, 0.50, 0.50},
            {0.50, 0.18, 0.50},
            {0.50, 0.82, 0.50},
            {0.18, 0.50, 0.50},
            {0.82, 0.50, 0.50},
            {0.50, 0.50, 0.18},
            {0.50, 0.50, 0.82}
    };

    private static final Comparator<WorksiteScanCandidate> SCAN_ORDER = Comparator
            .comparingInt((WorksiteScanCandidate candidate) -> candidate.style().priority()).reversed()
            .thenComparingDouble(WorksiteScanCandidate::distanceSquared);

    private final WorksiteBlockInspector blockInspector;
    private final PriorityQueue<WorksiteScanCandidate> candidateBuffer = new PriorityQueue<>(
            WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES,
            SCAN_ORDER.reversed());
    private final ArrayList<WorksiteScanCandidate> orderedBuffer = new ArrayList<>(
            WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES);
    private final ArrayList<WorksiteVisibleTarget> visibleBuffer = new ArrayList<>(
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS);
    private final WorksiteScanCandidate[] candidatePool = createCandidatePool();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos visibilityCursor = new BlockPos.MutableBlockPos();
    private final boolean[] loadedChunkBuffer = new boolean[
            WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES];

    private int nextCandidateSlot;
    private int remainingLineOfSightRays;

    WorksiteScanner(WorksiteBlockInspector blockInspector) {
        this.blockInspector = blockInspector;
    }

    List<WorksiteVisibleTarget> scan(
            Minecraft client,
            LocalFeatureConfig config,
            Set<BlockInspectionCategory> activeCategories) {
        if (client == null || client.player == null || client.level == null
                || activeCategories == null || activeCategories.isEmpty()) {
            return List.of();
        }

        int horizontalRadius = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                config.worksiteVisibilityHorizontalRadius);
        int verticalRadius = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                config.worksiteVisibilityVerticalRadius);
        int overlayLimit = WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                config.worksiteVisibilityMaxOverlayResults);
        BlockPos origin = client.player.blockPosition();
        Vec3 eyePosition = client.player.getEyePosition();

        candidateBuffer.clear();
        orderedBuffer.clear();
        visibleBuffer.clear();
        nextCandidateSlot = 0;
        remainingLineOfSightRays = WorksiteVisibilityBudgetPolicy.MAX_LINE_OF_SIGHT_RAYS_PER_SCAN;

        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        int minX = originX - horizontalRadius;
        int maxX = originX + horizontalRadius;
        int minZ = originZ - horizontalRadius;
        int maxZ = originZ + horizontalRadius;
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        int chunkSpanX = maxChunkX - minChunkX + 1;
        int chunkSpanZ = maxChunkZ - minChunkZ + 1;
        int chunkCount = chunkSpanX * chunkSpanZ;
        if (chunkCount > loadedChunkBuffer.length) return List.of();

        int chunkIndex = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                loadedChunkBuffer[chunkIndex++] = client.level.getChunkSource().hasChunk(chunkX, chunkZ);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            int loadedRow = ((z >> 4) - minChunkZ) * chunkSpanX;
            for (int x = minX; x <= maxX; x++) {
                int loadedIndex = loadedRow + ((x >> 4) - minChunkX);
                if (!loadedChunkBuffer[loadedIndex]) continue;
                for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                    cursor.set(x, originY + yOffset, z);
                    collectCandidate(
                            client,
                            config,
                            eyePosition,
                            cursor,
                            activeCategories,
                            candidateBuffer);
                }
            }
        }

        orderedBuffer.addAll(candidateBuffer);
        orderedBuffer.sort(SCAN_ORDER);
        for (WorksiteScanCandidate candidate : orderedBuffer) {
            if (visibleBuffer.size() >= overlayLimit || remainingLineOfSightRays <= 0) break;
            WorksiteVisibleTarget target = materializeVisibleTarget(client, eyePosition, candidate);
            if (target != null) visibleBuffer.add(target);
        }
        return List.copyOf(visibleBuffer);
    }

    private void collectCandidate(
            Minecraft client,
            LocalFeatureConfig config,
            Vec3 eyePosition,
            BlockPos position,
            Set<BlockInspectionCategory> activeCategories,
            PriorityQueue<WorksiteScanCandidate> candidates) {
        BlockState state = client.level.getBlockState(position);
        WorksiteBlockDescriptor descriptor = blockInspector.describe(state);
        BlockInspectionCategory category =
                blockInspector.resolveActiveCategory(descriptor, activeCategories);
        if (!BlockInspectionPolicy.isScanCategory(category)) return;
        if (!VisualTargetSelectionPolicy.matchesEnabled(
                config.visualTargetMask,
                descriptor.id(),
                category)) return;

        VisualAssistanceStylePolicy.OverlayStyle style = descriptor.styleFor(category);
        if (!style.visible()) return;

        double dx = position.getX() + 0.5 - eyePosition.x;
        double dy = position.getY() + 0.5 - eyePosition.y;
        double dz = position.getZ() + 0.5 - eyePosition.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        WorksiteScanCandidate weakest = candidates.peek();
        int weakestPriority = weakest == null ? Integer.MIN_VALUE : weakest.style().priority();
        double weakestDistanceSquared = weakest == null
                ? Double.POSITIVE_INFINITY
                : weakest.distanceSquared();
        if (!WorksiteCandidateRetentionPolicy.shouldRetain(
                candidates.size(),
                WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES,
                style.priority(),
                distanceSquared,
                weakestPriority,
                weakestDistanceSquared)) return;

        WorksiteScanCandidate candidate;
        if (candidates.size() == WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES) {
            candidate = candidates.poll();
        } else {
            candidate = candidatePool[nextCandidateSlot++];
        }
        candidate.assign(
                position.getX(),
                position.getY(),
                position.getZ(),
                state,
                descriptor.id(),
                category,
                style,
                distanceSquared);
        candidates.add(candidate);
    }

    private WorksiteVisibleTarget materializeVisibleTarget(
            Minecraft client,
            Vec3 eyePosition,
            WorksiteScanCandidate candidate) {
        if (remainingLineOfSightRays <= 0) return null;
        visibilityCursor.set(candidate.x(), candidate.y(), candidate.z());
        if (!lineOfSight(client, eyePosition, visibilityCursor, candidate.category())) return null;

        BlockPos position = new BlockPos(candidate.x(), candidate.y(), candidate.z());
        WorksiteMaterializedInspection inspection = blockInspector.materialize(
                candidate.state(), candidate.blockId(), candidate.category());
        return new WorksiteVisibleTarget(
                position,
                inspection.presentation(),
                inspection.orientation(),
                candidate.style(),
                candidate.distanceSquared());
    }

    private boolean lineOfSight(
            Minecraft client,
            Vec3 eyePosition,
            BlockPos position,
            BlockInspectionCategory category) {
        for (double[] sample : samplesFor(category)) {
            if (remainingLineOfSightRays <= 0) return false;
            remainingLineOfSightRays--;
            Vec3 target = new Vec3(
                    position.getX() + sample[0],
                    position.getY() + sample[1],
                    position.getZ() + sample[2]);
            BlockHitResult result = client.level.clip(new ClipContext(
                    eyePosition,
                    target,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    client.player));
            if (result.getType() == HitResult.Type.BLOCK
                    && result.getBlockPos().equals(position)) return true;
        }
        return false;
    }

    private static double[][] samplesFor(BlockInspectionCategory category) {
        return switch (category) {
            case TECHNICAL_TRACE -> THIN_TECHNICAL_SAMPLES;
            case HIDDEN_SURFACE -> SHAPED_BLOCK_SAMPLES;
            case MATERIAL_HIGHLIGHT, NETHER_PALETTE, NONE -> SOLID_SAMPLES;
        };
    }

    private static WorksiteScanCandidate[] createCandidatePool() {
        WorksiteScanCandidate[] pool =
                new WorksiteScanCandidate[WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES];
        for (int index = 0; index < pool.length; index++) {
            pool[index] = new WorksiteScanCandidate();
        }
        return pool;
    }
}
