package dev.chise.chisetweaks.feature.placement;

import dev.chise.chisetweaks.config.FeatureSwitches;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the closest air block directly outside the player's collision box.
 * The result is only substituted for a normal MISS during the user's vanilla use action.
 */
public final class AirPlacementTarget {
    private static final float VERTICAL_LOOK_THRESHOLD = 60.0F;

    private AirPlacementTarget() {}

    public static @Nullable BlockHitResult resolve(
            @Nullable LocalPlayer player,
            @Nullable ClientLevel level,
            @Nullable HitResult currentHit) {
        if (!FeatureSwitches.AIR_PLACEMENT.getBooleanValue()
                || player == null
                || level == null
                || currentHit == null
                || currentHit.getType() != HitResult.Type.MISS
                || player.isSpectator()
                || !hasBlockItem(player.getMainHandItem(), player.getOffhandItem())) {
            return null;
        }

        BlockPos target = targetPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getEyeY(),
                player.getBoundingBox(),
                player.getXRot(),
                player.getDirection());
        int chunkX = target.getX() >> 4;
        int chunkZ = target.getZ() >> 4;
        if (level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null
                || !level.getBlockState(target).isAir()) {
            return null;
        }

        Direction face = clickedFace(player.getXRot(), player.getDirection());
        return new BlockHitResult(faceCenter(target, face), face, target, false);
    }

    static BlockPos targetPosition(
            double x,
            double y,
            double z,
            double eyeY,
            AABB bounds,
            float pitch,
            Direction horizontalDirection) {
        if (pitch >= VERTICAL_LOOK_THRESHOLD) {
            return BlockPos.containing(x, y - 1.0D, z);
        }
        if (pitch <= -VERTICAL_LOOK_THRESHOLD) {
            return BlockPos.containing(x, Math.ceil(bounds.maxY), z);
        }

        int targetY = (int) Math.floor(eyeY);
        return switch (horizontalDirection) {
            case EAST -> new BlockPos((int) Math.ceil(bounds.maxX), targetY, (int) Math.floor(z));
            case WEST -> new BlockPos((int) Math.floor(bounds.minX) - 1, targetY, (int) Math.floor(z));
            case SOUTH -> new BlockPos((int) Math.floor(x), targetY, (int) Math.ceil(bounds.maxZ));
            case NORTH -> new BlockPos((int) Math.floor(x), targetY, (int) Math.floor(bounds.minZ) - 1);
            default -> BlockPos.containing(x, eyeY, z);
        };
    }

    static Direction clickedFace(float pitch, Direction horizontalDirection) {
        if (pitch >= VERTICAL_LOOK_THRESHOLD) return Direction.UP;
        if (pitch <= -VERTICAL_LOOK_THRESHOLD) return Direction.DOWN;
        return horizontalDirection.getOpposite();
    }

    private static Vec3 faceCenter(BlockPos pos, Direction face) {
        return new Vec3(
                pos.getX() + 0.5D + 0.5D * face.getStepX(),
                pos.getY() + 0.5D + 0.5D * face.getStepY(),
                pos.getZ() + 0.5D + 0.5D * face.getStepZ());
    }

    private static boolean hasBlockItem(ItemStack mainHand, ItemStack offHand) {
        return mainHand.getItem() instanceof BlockItem || offHand.getItem() instanceof BlockItem;
    }
}
