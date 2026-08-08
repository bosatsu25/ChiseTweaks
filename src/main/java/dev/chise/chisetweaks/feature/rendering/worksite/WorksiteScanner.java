package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteCandidateRetentionPolicy;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
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

/** Performs the bounded, loaded-chunk-only scan for visible worksite targets. */
final class WorksiteScanner {
    private static final Comparator<ScanCandidate> SCAN_ORDER = Comparator
            .comparingInt((ScanCandidate candidate) -> candidate.style().priority()).reversed()
            .thenComparingDouble(ScanCandidate::distanceSquared);

    private final WorksiteBlockInspector blockInspector;

    WorksiteScanner(WorksiteBlockInspector blockInspector) {
        this.blockInspector = blockInspector;
    }

    List<WorksiteVisibleTarget> scan(
            Minecraft client,
            LocalFeatureConfig config,
            Set<BlockInspectionCategory> activeCategories) {
        int horizontalRadius = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(
                config.worksiteVisibilityHorizontalRadius);
        int verticalRadius = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(
                config.worksiteVisibilityVerticalRadius);
        int overlayLimit = WorksiteVisibilityBudgetPolicy.clampOverlayResults(
                config.worksiteVisibilityMaxOverlayResults);
        BlockPos origin = client.player.blockPosition();
        Vec3 eyePosition = client.player.getEyePosition();
        PriorityQueue<ScanCandidate> candidates = new PriorityQueue<>(
                WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES,
                SCAN_ORDER.reversed());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
            int y = originY + yOffset;
            for (int zOffset = -horizontalRadius; zOffset <= horizontalRadius; zOffset++) {
                int z = originZ + zOffset;
                for (int xOffset = -horizontalRadius; xOffset <= horizontalRadius; xOffset++) {
                    cursor.set(originX + xOffset, y, z);
                    collectCandidate(client, eyePosition, cursor, activeCategories, candidates);
                }
            }
        }

        ArrayList<ScanCandidate> orderedCandidates = new ArrayList<>(candidates);
        orderedCandidates.sort(SCAN_ORDER);
        ArrayList<WorksiteVisibleTarget> visible = new ArrayList<>(overlayLimit);
        for (ScanCandidate candidate : orderedCandidates) {
            if (visible.size() >= overlayLimit) break;
            WorksiteVisibleTarget target = materializeVisibleTarget(client, eyePosition, candidate);
            if (target != null) visible.add(target);
        }
        return List.copyOf(visible);
    }

    private void collectCandidate(
            Minecraft client,
            Vec3 eyePosition,
            BlockPos position,
            Set<BlockInspectionCategory> activeCategories,
            PriorityQueue<ScanCandidate> candidates) {
        if (!client.level.hasChunkAt(position)) return;
        BlockState state = client.level.getBlockState(position);
        WorksiteBlockDescriptor descriptor = blockInspector.describe(state);
        BlockInspectionCategory category =
                blockInspector.resolveActiveCategory(descriptor, activeCategories);
        if (!BlockInspectionPolicy.isScanCategory(category)) return;

        VisualAssistanceStylePolicy.OverlayStyle style = descriptor.styleFor(category);
        if (!style.visible()) return;

        double dx = position.getX() + 0.5 - eyePosition.x;
        double dy = position.getY() + 0.5 - eyePosition.y;
        double dz = position.getZ() + 0.5 - eyePosition.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        ScanCandidate weakest = candidates.peek();
        int weakestPriority = weakest == null ? Integer.MIN_VALUE : weakest.style().priority();
        double weakestDistanceSquared = weakest == null ? Double.POSITIVE_INFINITY : weakest.distanceSquared();
        if (!WorksiteCandidateRetentionPolicy.shouldRetain(
                candidates.size(),
                WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES,
                style.priority(),
                distanceSquared,
                weakestPriority,
                weakestDistanceSquared)) return;

        ScanCandidate candidate = new ScanCandidate(
                position.getX(),
                position.getY(),
                position.getZ(),
                state,
                descriptor.id(),
                category,
                style,
                distanceSquared);
        if (candidates.size() == WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES) {
            candidates.poll();
        }
        candidates.add(candidate);
    }

    private WorksiteVisibleTarget materializeVisibleTarget(
            Minecraft client,
            Vec3 eyePosition,
            ScanCandidate candidate) {
        BlockPos position = new BlockPos(candidate.x(), candidate.y(), candidate.z());
        if (!lineOfSight(client, eyePosition, position)) return null;

        WorksiteMaterializedInspection inspection = blockInspector.materialize(
                candidate.state(), candidate.blockId(), candidate.category());
        return new WorksiteVisibleTarget(
                position,
                inspection.presentation(),
                inspection.orientation(),
                candidate.style(),
                candidate.distanceSquared());
    }

    private static boolean lineOfSight(Minecraft client, Vec3 eyePosition, BlockPos position) {
        Vec3 center = Vec3.atCenterOf(position);
        BlockHitResult result = client.level.clip(new ClipContext(
                eyePosition,
                center,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                client.player));
        return result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(position);
    }

    private record ScanCandidate(
            int x,
            int y,
            int z,
            BlockState state,
            String blockId,
            BlockInspectionCategory category,
            VisualAssistanceStylePolicy.OverlayStyle style,
            double distanceSquared) {}
}
