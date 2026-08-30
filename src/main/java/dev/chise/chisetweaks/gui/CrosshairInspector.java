package dev.chise.chisetweaks.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Objects;

/**
 * Tracks the existing Minecraft crosshair target for lightweight Block Info and Placement Assist.
 *
 * <p>This class owns hit lifecycle, cache invalidation and the immutable snapshot boundary only.
 * It intentionally does not compute Chise feature-impact diagnostics.</p>
 */
final class CrosshairInspector {
    private static final Snapshot NO_TARGET = new Snapshot(
            HitResult.Type.MISS, "", List.of(), null, null,
            PlacementInspector.NONE, null, false);

    private HitResult.Type cachedKind;
    private BlockState cachedBlockState;
    private EntityType<?> cachedEntityType;
    private Snapshot snapshot = NO_TARGET;

    Snapshot snapshot() {
        return snapshot;
    }

    void invalidate() {
        cachedKind = null;
    }

    boolean refresh(Minecraft client) {
        try {
            return refreshSafely(client);
        } catch (RuntimeException | LinkageError ignored) {
            boolean changed = snapshot != NO_TARGET;
            cachedKind = HitResult.Type.MISS;
            cachedBlockState = null;
            cachedEntityType = null;
            snapshot = NO_TARGET;
            return changed;
        }
    }

    private boolean refreshSafely(Minecraft client) {
        if (client == null || client.level == null || client.hitResult == null) {
            return updateNoTarget();
        }
        HitResult hit = client.hitResult;
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return updateBlock(client, blockHit, client.level.getBlockState(blockHit.getBlockPos()));
        }
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            return updateEntity(entityHit.getEntity());
        }
        return updateNoTarget();
    }

    private boolean updateNoTarget() {
        if (cachedKind == HitResult.Type.MISS) return false;
        cachedKind = HitResult.Type.MISS;
        cachedBlockState = null;
        cachedEntityType = null;
        snapshot = NO_TARGET;
        return true;
    }

    private boolean updateBlock(Minecraft client, BlockHitResult hit, BlockState state) {
        PlacementInspector comparison = PlacementInspector.activeAt(client.level, hit.getBlockPos());
        ItemStack stack = client.player == null ? null : client.player.getMainHandItem();
        PlacementInspector.PlacementProbe probe = PlacementInspector.placementProbe(
                client.level,
                client.player,
                InteractionHand.MAIN_HAND,
                stack,
                hit,
                true);
        BlockState livePrediction = probe == null ? null : probe.predictedState();
        Direction clickedFace = probe == null ? null : hit.getDirection();
        boolean upperClick = probe != null
                && hit.getLocation().y - hit.getBlockPos().getY() > 0.5D;
        BlockState predictedPlacement = comparison == null ? livePrediction : comparison.predictedState;
        BlockState actualPlacement = comparison == null ? null : comparison.actualState;
        int placementResult = comparison == null
                ? PlacementInspector.NONE
                : PlacementInspector.compare(predictedPlacement, actualPlacement);

        if (cachedKind == HitResult.Type.BLOCK
                && cachedBlockState == state
                && snapshot.predictedPlacement() == predictedPlacement
                && snapshot.actualPlacement() == actualPlacement
                && snapshot.placementResult() == placementResult
                && snapshot.clickedFace() == clickedFace
                && snapshot.upperClick() == upperClick) {
            return false;
        }

        cachedKind = HitResult.Type.BLOCK;
        cachedBlockState = state;
        cachedEntityType = null;
        snapshot = CrosshairSnapshotPolicy.blockSnapshot(
                state,
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
        return true;
    }

    private boolean updateEntity(Entity entity) {
        EntityType<?> type = entity == null ? null : entity.getType();
        if (cachedKind == HitResult.Type.ENTITY && cachedEntityType == type) return false;

        cachedKind = HitResult.Type.ENTITY;
        cachedBlockState = null;
        cachedEntityType = type;
        snapshot = CrosshairSnapshotPolicy.entitySnapshot(entity);
        return true;
    }

    record Snapshot(
            HitResult.Type targetKind,
            String targetId,
            List<String> stateProperties,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            Direction clickedFace,
            boolean upperClick) {
        Snapshot {
            Objects.requireNonNull(targetKind);
            targetId = targetId == null ? "" : targetId;
            stateProperties = List.copyOf(Objects.requireNonNull(stateProperties));
            if (targetKind == HitResult.Type.MISS && !targetId.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (targetKind != HitResult.Type.MISS && targetId.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (targetKind != HitResult.Type.BLOCK && !stateProperties.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (placementResult != PlacementInspector.NONE && predictedPlacement == null) {
                throw new IllegalArgumentException();
            }
            if (actualPlacement != null && placementResult == PlacementInspector.NONE) {
                throw new IllegalArgumentException();
            }
        }

        static Snapshot noTarget() {
            return NO_TARGET;
        }
    }
}
