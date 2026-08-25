package dev.chise.chisetweaks.feature.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AirPlacementTargetTest {
    private static final AABB BOUNDS = new AABB(9.2D, 64.0D, 19.2D, 9.8D, 65.8D, 19.8D);

    @Test
    void horizontalTargetsStayImmediatelyOutsidePlayerBoundsAtEyeHeight() {
        assertEquals(new BlockPos(10, 65, 19), target(0.0F, Direction.EAST));
        assertEquals(new BlockPos(8, 65, 19), target(0.0F, Direction.WEST));
        assertEquals(new BlockPos(9, 65, 20), target(0.0F, Direction.SOUTH));
        assertEquals(new BlockPos(9, 65, 18), target(0.0F, Direction.NORTH));
    }

    @Test
    void verticalBoundaryUsesFloorAndCeilingWithoutIntersectingPlayer() {
        assertEquals(new BlockPos(9, 63, 19), target(60.0F, Direction.NORTH));
        assertEquals(new BlockPos(9, 66, 19), target(-60.0F, Direction.NORTH));
        assertEquals(new BlockPos(9, 65, 18), target(59.99F, Direction.NORTH));
        assertEquals(new BlockPos(9, 65, 18), target(-59.99F, Direction.NORTH));
    }

    @Test
    void clickedFacePointsBackTowardPlayerForHorizontalAndVerticalPlacement() {
        assertEquals(Direction.WEST, AirPlacementTarget.clickedFace(0.0F, Direction.EAST));
        assertEquals(Direction.EAST, AirPlacementTarget.clickedFace(0.0F, Direction.WEST));
        assertEquals(Direction.UP, AirPlacementTarget.clickedFace(60.0F, Direction.NORTH));
        assertEquals(Direction.DOWN, AirPlacementTarget.clickedFace(-60.0F, Direction.NORTH));
    }

    private static BlockPos target(float pitch, Direction direction) {
        return AirPlacementTarget.targetPosition(
                9.5D, 64.0D, 19.5D, 65.62D, BOUNDS, pitch, direction);
    }
}
