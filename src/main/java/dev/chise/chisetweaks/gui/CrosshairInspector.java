package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Objects;

/**
 * Observes the existing Minecraft crosshair hit and maintains one privacy-safe Inspector snapshot.
 *
 * <p>Placement rules and snapshot derivation live in dedicated policies; this class owns only
 * hit lifecycle, cache invalidation and the immutable snapshot boundary.</p>
 */
final class CrosshairInspector {
    private static final Snapshot NO_TARGET = new Snapshot(
            HitResult.Type.MISS, "", List.of(), null, List.of(), null, null,
            PlacementInspector.NONE, null, false);

    private HitResult.Type cachedKind;
    private BlockState cachedBlockState;
    private EntityType<?> cachedEntityType;
    private boolean cachedSelf;
    private long cachedFeatureMask = Long.MIN_VALUE;
    private int cachedVisualTargetMask = Integer.MIN_VALUE;
    private boolean cachedWorldOverlay;
    private boolean cachedInNether;
    private long cachedFilterRevision = Long.MIN_VALUE;
    private long cachedOreRevision = Long.MIN_VALUE;
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
            BlockState state = client.level.getBlockState(blockHit.getBlockPos());
            return updateBlock(client, blockHit, state, Level.NETHER.equals(client.level.dimension()));
        }
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            return updateEntity(client, entityHit.getEntity());
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

    private boolean updateBlock(
            Minecraft client,
            BlockHitResult hit,
            BlockState state,
            boolean inNether) {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        long featureMask = CrosshairSnapshotPolicy.currentEnabledFeatureMask();
        long filterRevision = BuilderFocusVisibility.revision();
        long oreRevision = OreHighlightResolver.revision();
        PlacementInspector comparison = PlacementInspector.activeAt(
                client.level, hit.getBlockPos());
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
                && cachedFeatureMask == featureMask
                && cachedVisualTargetMask == local.visualTargetMask
                && cachedWorldOverlay == local.worksiteVisibilityWorldOverlay
                && cachedInNether == inNether
                && cachedFilterRevision == filterRevision
                && cachedOreRevision == oreRevision
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
        cachedFeatureMask = featureMask;
        cachedVisualTargetMask = local.visualTargetMask;
        cachedWorldOverlay = local.worksiteVisibilityWorldOverlay;
        cachedInNether = inNether;
        cachedFilterRevision = filterRevision;
        cachedOreRevision = oreRevision;
        snapshot = CrosshairSnapshotPolicy.blockSnapshot(
                state,
                featureMask,
                local.visualTargetMask,
                local.worksiteVisibilityWorldOverlay,
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
        return true;
    }

    private boolean updateEntity(Minecraft client, Entity entity) {
        EntityType<?> type = entity == null ? null : entity.getType();
        boolean self = entity != null && entity == client.player;
        long filterRevision = BuilderFocusVisibility.revision();
        if (cachedKind == HitResult.Type.ENTITY
                && cachedEntityType == type
                && cachedSelf == self
                && cachedFilterRevision == filterRevision) {
            return false;
        }

        cachedKind = HitResult.Type.ENTITY;
        cachedBlockState = null;
        cachedEntityType = type;
        cachedSelf = self;
        cachedFilterRevision = filterRevision;
        snapshot = CrosshairSnapshotPolicy.entitySnapshot(entity);
        return true;
    }

    record Snapshot(
            HitResult.Type targetKind,
            String targetId,
            List<String> stateProperties,
            BuilderFocusVisibility.FilterDecision filterDecision,
            List<FeatureDefinition> responsibleFeatures,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            Direction clickedFace,
            boolean upperClick) {
        Snapshot {
            Objects.requireNonNull(targetKind);
            targetId = targetId == null ? "" : targetId;
            stateProperties = List.copyOf(Objects.requireNonNull(stateProperties));
            responsibleFeatures = List.copyOf(Objects.requireNonNull(responsibleFeatures));
            if (targetKind == HitResult.Type.MISS && (!targetId.isEmpty() || filterDecision != null)) {
                throw new IllegalArgumentException();
            }
            if (targetKind != HitResult.Type.MISS && (targetId.isEmpty() || filterDecision == null)) {
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
