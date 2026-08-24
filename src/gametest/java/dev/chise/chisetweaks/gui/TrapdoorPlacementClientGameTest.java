package dev.chise.chisetweaks.gui;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 実際のvanilla配置結果を、製品側previewのoracleとして比較するclient integration test。 */
public final class TrapdoorPlacementClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runOnServer(server -> {
                if (server.getPlayerList().getPlayers().isEmpty()) {
                    throw new AssertionError("singleplayer test player is unavailable");
                }
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = server.overworld();
                BlockPos origin = player.blockPosition().offset(8, 2, 8);

                require(CrosshairInspector.supportsPlacementPreview(Blocks.OAK_TRAPDOOR),
                        "trapdoor family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.OAK_SLAB),
                        "slab family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.OAK_LOG),
                        "log family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.OAK_WOOD),
                        "wood family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.CRIMSON_STEM),
                        "stem family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.CRIMSON_HYPHAE),
                        "hyphae family must be supported");
                require(CrosshairInspector.supportsPlacementPreview(Blocks.OCHRE_FROGLIGHT),
                        "froglight family must be supported");
                require(!CrosshairInspector.supportsPlacementPreview(Blocks.STONE),
                        "ordinary block must remain unsupported");
                require(!CrosshairInspector.supportsPlacementPreview(Blocks.QUARTZ_PILLAR),
                        "unlisted axis block must remain unsupported");

                BlockHitResult unsupportedHit = new BlockHitResult(
                        Vec3.atCenterOf(origin), Direction.UP, origin, false);
                require(CrosshairInspector.predictPlacementState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Items.STICK),
                                unsupportedHit,
                                true) == null,
                        "non-BlockItem must not produce a preview");
                require(CrosshairInspector.predictPlacementState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Blocks.STONE),
                                unsupportedHit,
                                true) == null,
                        "unsupported BlockItem must not produce a preview");
                require(CrosshairInspector.predictPlacementState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Blocks.OAK_LOG),
                                unsupportedHit,
                                false) == null,
                        "disabled axis preview must not produce a descriptor");
                require(CrosshairInspector.predictPlacementState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Blocks.OAK_SLAB),
                                unsupportedHit,
                                false) == null,
                        "disabled slab preview must not produce a descriptor");

                assertTrapdoorPlacement(level, player, origin, 0, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.25D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertTrapdoorPlacement(level, player, origin, 1, Blocks.OAK_TRAPDOOR,
                        Direction.EAST, 0.25D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertTrapdoorPlacement(level, player, origin, 2, Blocks.OAK_TRAPDOOR,
                        Direction.SOUTH, 0.25D, TargetKind.SOLID, false, Direction.NORTH, true);
                assertTrapdoorPlacement(level, player, origin, 3, Blocks.OAK_TRAPDOOR,
                        Direction.WEST, 0.25D, TargetKind.SOLID, false, Direction.NORTH, true);
                assertTrapdoorPlacement(level, player, origin, 4, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.SOLID, false, Direction.EAST, true);
                assertTrapdoorPlacement(level, player, origin, 5, Blocks.OAK_TRAPDOOR,
                        Direction.DOWN, 0.0D, TargetKind.SOLID, false, Direction.WEST, true);
                assertTrapdoorPlacement(level, player, origin, 6, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.75D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertTrapdoorPlacement(level, player, origin, 7, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.25D, TargetKind.SOLID, true, Direction.SOUTH, true);
                assertTrapdoorPlacement(level, player, origin, 8, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.REPLACEABLE, false, Direction.NORTH, true);
                assertTrapdoorPlacement(level, player, origin, 9, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.WATER, false, Direction.SOUTH, true);
                assertTrapdoorPlacement(level, player, origin, 10, Blocks.OAK_TRAPDOOR,
                        Direction.EAST, 0.25D, TargetKind.BLOCKED, false, Direction.SOUTH, false);
                assertTrapdoorPlacement(level, player, origin, 11, Blocks.IRON_TRAPDOOR,
                        Direction.WEST, 0.75D, TargetKind.SOLID, false, Direction.NORTH, true);

                assertAxisPlacement(level, player, origin, 12, Blocks.OAK_LOG,
                        Direction.UP, Direction.Axis.Y);
                assertAxisPlacement(level, player, origin, 13, Blocks.OAK_WOOD,
                        Direction.DOWN, Direction.Axis.Y);
                assertAxisPlacement(level, player, origin, 14, Blocks.CRIMSON_STEM,
                        Direction.NORTH, Direction.Axis.Z);
                assertAxisPlacement(level, player, origin, 15, Blocks.CRIMSON_HYPHAE,
                        Direction.SOUTH, Direction.Axis.Z);
                assertAxisPlacement(level, player, origin, 16, Blocks.OAK_LOG,
                        Direction.EAST, Direction.Axis.X);
                assertAxisPlacement(level, player, origin, 17, Blocks.OAK_LOG,
                        Direction.WEST, Direction.Axis.X);
                assertAxisPlacement(level, player, origin, 18, Blocks.OCHRE_FROGLIGHT,
                        Direction.UP, Direction.Axis.Y);
                assertAxisPlacement(level, player, origin, 19, Blocks.VERDANT_FROGLIGHT,
                        Direction.NORTH, Direction.Axis.Z);
                assertAxisPlacement(level, player, origin, 20, Blocks.PEARLESCENT_FROGLIGHT,
                        Direction.EAST, Direction.Axis.X);

                assertSlabPlacement(level, player, origin, 21, Direction.NORTH, 0.25D,
                        TargetKind.SOLID, SlabType.BOTTOM, false);
                assertSlabPlacement(level, player, origin, 22, Direction.NORTH, 0.75D,
                        TargetKind.SOLID, SlabType.TOP, false);
                assertSlabPlacement(level, player, origin, 23, Direction.UP, 1.0D,
                        TargetKind.SOLID, SlabType.BOTTOM, false);
                assertSlabPlacement(level, player, origin, 24, Direction.DOWN, 0.0D,
                        TargetKind.SOLID, SlabType.TOP, false);
                assertSlabPlacement(level, player, origin, 25, Direction.UP, 1.0D,
                        TargetKind.WATER, SlabType.BOTTOM, true);
                assertSlabPlacement(level, player, origin, 26, Direction.UP, 1.0D,
                        TargetKind.REPLACEABLE, SlabType.BOTTOM, false);
            });
        }
    }

    private static void assertTrapdoorPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block trapdoor,
            Direction face,
            double clickY,
            TargetKind targetKind,
            boolean sneaking,
            Direction playerFacing,
            boolean expectedPossible) {
        PlacementResult result = previewAndPlace(
                level, player, origin, index, trapdoor, face, clickY, targetKind, sneaking, playerFacing);
        require((result.predicted() != null) == expectedPossible,
                "preview possibility mismatch for " + targetKind);
        require(result.actuallyPlaced() == expectedPossible,
                "vanilla placement possibility mismatch for " + targetKind);
        if (!expectedPossible) return;
        BlockState predicted = result.predicted();
        BlockState actual = result.actual();
        require(predicted.getValue(TrapDoorBlock.FACING) == actual.getValue(TrapDoorBlock.FACING),
                "facing mismatch");
        require(predicted.getValue(TrapDoorBlock.HALF) == actual.getValue(TrapDoorBlock.HALF),
                "half mismatch");
        require(predicted.getValue(TrapDoorBlock.OPEN).equals(actual.getValue(TrapDoorBlock.OPEN)),
                "open mismatch");
        require(predicted.getValue(TrapDoorBlock.POWERED).equals(actual.getValue(TrapDoorBlock.POWERED)),
                "powered mismatch");
        require(predicted.getValue(TrapDoorBlock.WATERLOGGED)
                        .equals(actual.getValue(TrapDoorBlock.WATERLOGGED)),
                "waterlogged mismatch");
        List<String> properties = CrosshairInspector.placementStateProperties(predicted);
        require(properties.size() == 5
                        && properties.stream().anyMatch(value -> value.startsWith("facing="))
                        && properties.stream().anyMatch(value -> value.startsWith("half="))
                        && properties.stream().anyMatch(value -> value.startsWith("open="))
                        && properties.stream().anyMatch(value -> value.startsWith("powered="))
                        && properties.stream().anyMatch(value -> value.startsWith("waterlogged=")),
                "trapdoor preview must expose only five placement properties");
    }

    private static void assertAxisPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block block,
            Direction face,
            Direction.Axis expectedAxis) {
        PlacementResult result = previewAndPlace(
                level, player, origin, index, block, face, 0.5D,
                TargetKind.SOLID, false, Direction.SOUTH);
        require(result.predicted() != null && result.actuallyPlaced(),
                "axis block must be placeable on " + face);
        Direction.Axis predicted = result.predicted().getValue(BlockStateProperties.AXIS);
        Direction.Axis actual = result.actual().getValue(BlockStateProperties.AXIS);
        require(predicted == actual, "axis mismatch for " + block);
        require(actual == expectedAxis, "unexpected vanilla axis for " + face);
        require(CrosshairInspector.placementStateProperties(result.predicted())
                        .equals(List.of("axis=" + expectedAxis.getSerializedName())),
                "axis preview must expose only axis");
    }

    private static void assertSlabPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Direction face,
            double clickY,
            TargetKind targetKind,
            SlabType expectedType,
            boolean expectedWaterlogged) {
        PlacementResult result = previewAndPlace(
                level, player, origin, index, Blocks.OAK_SLAB, face, clickY,
                targetKind, false, Direction.SOUTH);
        require(result.predicted() != null && result.actuallyPlaced(),
                "slab must be placeable for " + targetKind + " on " + face);
        SlabType predictedType = result.predicted().getValue(BlockStateProperties.SLAB_TYPE);
        SlabType actualType = result.actual().getValue(BlockStateProperties.SLAB_TYPE);
        boolean predictedWaterlogged = result.predicted().getValue(BlockStateProperties.WATERLOGGED);
        boolean actualWaterlogged = result.actual().getValue(BlockStateProperties.WATERLOGGED);
        require(predictedType == actualType, "slab type mismatch");
        require(predictedWaterlogged == actualWaterlogged, "slab waterlogging mismatch");
        require(actualType == expectedType, "unexpected vanilla slab type");
        require(actualWaterlogged == expectedWaterlogged, "unexpected vanilla slab waterlogging");
        require(CrosshairInspector.placementStateProperties(result.predicted()).equals(List.of(
                        "type=" + expectedType.getSerializedName(),
                        "waterlogged=" + expectedWaterlogged)),
                "slab preview must expose only type and waterlogged");
    }

    private static PlacementResult previewAndPlace(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block block,
            Direction face,
            double clickY,
            TargetKind targetKind,
            boolean sneaking,
            Direction playerFacing) {
        BlockPos clicked = testPosition(origin, index);
        clear(level, clicked);
        switch (targetKind) {
            case SOLID, BLOCKED -> level.setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState());
            case REPLACEABLE -> {
                level.setBlockAndUpdate(clicked.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(clicked, Blocks.SHORT_GRASS.defaultBlockState());
            }
            case WATER -> level.setBlockAndUpdate(clicked, Blocks.WATER.defaultBlockState());
        }
        if (targetKind == TargetKind.BLOCKED) {
            level.setBlockAndUpdate(clicked.relative(face), Blocks.STONE.defaultBlockState());
        }

        player.setYRot(playerFacing.toYRot());
        player.setXRot(0.0F);
        player.setShiftKeyDown(sneaking);
        try {
            BlockHitResult hit = new BlockHitResult(
                    hitLocation(clicked, face, clickY),
                    face,
                    clicked,
                    false);
            ItemStack previewStack = new ItemStack(block);
            BlockState predicted = CrosshairInspector.predictPlacementState(
                    level,
                    player,
                    InteractionHand.MAIN_HAND,
                    previewStack,
                    hit,
                    true);

            ItemStack actualStack = new ItemStack(block);
            BlockPlaceContext actualContext = new BlockPlaceContext(
                    level,
                    player,
                    InteractionHand.MAIN_HAND,
                    actualStack,
                    hit);
            BlockPos actualPos = actualContext.getClickedPos();
            ((BlockItem) actualStack.getItem()).place(actualContext);
            BlockState actual = level.getBlockState(actualPos);
            return new PlacementResult(predicted, actual, actual.getBlock() == block);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    private static BlockPos testPosition(BlockPos origin, int index) {
        return origin.offset((index % 8) * 3, 0, (index / 8) * 3);
    }

    private static Vec3 hitLocation(BlockPos pos, Direction face, double clickY) {
        double x = pos.getX() + 0.5D + face.getStepX() * 0.5D;
        double y = pos.getY() + (face == Direction.UP ? 1.0D : face == Direction.DOWN ? 0.0D : clickY);
        double z = pos.getZ() + 0.5D + face.getStepZ() * 0.5D;
        return new Vec3(x, y, z);
    }

    private static void clear(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record PlacementResult(BlockState predicted, BlockState actual, boolean actuallyPlaced) {
    }

    private enum TargetKind {
        SOLID,
        REPLACEABLE,
        WATER,
        BLOCKED
    }
}
