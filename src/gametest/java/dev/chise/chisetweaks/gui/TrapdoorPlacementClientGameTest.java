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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                for (Block complex : new Block[]{
                        Blocks.OAK_STAIRS,
                        Blocks.WHITE_GLAZED_TERRACOTTA,
                        Blocks.OAK_FENCE_GATE,
                        Blocks.GRINDSTONE,
                        Blocks.BEEHIVE,
                        Blocks.BEE_NEST,
                        Blocks.CAMPFIRE}) {
                    require(CrosshairInspector.supportsPlacementPreview(complex),
                            "complex family must be supported: " + complex);
                }
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

                assertComplexPlacements(level, player, origin);
                assertComparisonLifecycle(
                        level, server.getLevel(Level.NETHER), player, origin, 52);
                assertPatternConsistency(
                        level, server.getLevel(Level.NETHER), origin.offset(40, 0, 40));
            });
        }
    }

    private static void assertComplexPlacements(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin) {
        Set<String> stairs = Set.of("facing", "half", "shape", "waterlogged");
        Set<Direction> stairFacings = new HashSet<>();
        int index = 27;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PlacementResult result = assertComplexPlacement(
                    level, player, origin, index++, Blocks.OAK_STAIRS,
                    Direction.UP, 1.0D, TargetKind.SOLID, facing,
                    stairs, stairs, true);
            stairFacings.add(result.actual().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        require(stairFacings.equals(Set.of(
                        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)),
                "stairs oracle must cover all horizontal facings");

        PlacementResult bottom = assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_STAIRS,
                Direction.NORTH, 0.25D, TargetKind.SOLID, Direction.SOUTH,
                stairs, stairs, true);
        PlacementResult top = assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_STAIRS,
                Direction.NORTH, 0.75D, TargetKind.SOLID, Direction.SOUTH,
                stairs, stairs, true);
        require(bottom.actual().getValue(BlockStateProperties.HALF) == Half.BOTTOM,
                "lower side click must exercise bottom stairs");
        require(top.actual().getValue(BlockStateProperties.HALF) == Half.TOP,
                "upper side click must exercise top stairs");

        PlacementResult waterlogged = assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_STAIRS,
                Direction.UP, 1.0D, TargetKind.WATER, Direction.NORTH,
                stairs, stairs, true);
        require(waterlogged.actual().getValue(BlockStateProperties.WATERLOGGED),
                "stairs water oracle must produce waterlogged actual state");
        assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_STAIRS,
                Direction.EAST, 0.5D, TargetKind.BLOCKED, Direction.NORTH,
                stairs, stairs, false);

        assertStairNeighborShape(level, player, origin, index++, Direction.NORTH, StairsShape.OUTER_LEFT);
        assertStairNeighborShape(level, player, origin, index++, Direction.SOUTH, StairsShape.INNER_LEFT);

        Set<String> facingOnly = Set.of("facing");
        Set<Direction> glazedFacings = new HashSet<>();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PlacementResult result = assertComplexPlacement(
                    level, player, origin, index++, Blocks.WHITE_GLAZED_TERRACOTTA,
                    Direction.UP, 1.0D, TargetKind.SOLID, facing,
                    facingOnly, facingOnly, true);
            glazedFacings.add(result.actual().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        require(glazedFacings.size() == 4, "glazed terracotta oracle must cover four facings");

        Set<String> gate = Set.of("facing", "in_wall", "open", "powered");
        assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_FENCE_GATE,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.WEST,
                gate, gate, true);
        assertComplexPlacement(
                level, player, origin, index++, Blocks.OAK_FENCE_GATE,
                Direction.UP, 1.0D, TargetKind.REPLACEABLE, Direction.EAST,
                gate, gate, true);

        Set<String> grindstone = Set.of("face", "facing");
        PlacementResult floor = assertComplexPlacement(
                level, player, origin, index++, Blocks.GRINDSTONE,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.NORTH,
                grindstone, grindstone, true);
        PlacementResult wall = assertComplexPlacement(
                level, player, origin, index++, Blocks.GRINDSTONE,
                Direction.NORTH, 0.5D, TargetKind.SOLID, Direction.EAST,
                grindstone, grindstone, true);
        PlacementResult ceiling = assertComplexPlacement(
                level, player, origin, index++, Blocks.GRINDSTONE,
                Direction.DOWN, 0.0D, TargetKind.SOLID, Direction.SOUTH,
                grindstone, grindstone, true);
        require(floor.actual().getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.FLOOR,
                "grindstone floor placement was not exercised");
        require(wall.actual().getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL,
                "grindstone wall placement was not exercised");
        require(ceiling.actual().getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING,
                "grindstone ceiling placement was not exercised");

        Set<String> hiveActual = Set.of("facing", "honey_level");
        assertComplexPlacement(
                level, player, origin, index++, Blocks.BEEHIVE,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.NORTH,
                facingOnly, hiveActual, true);
        assertComplexPlacement(
                level, player, origin, index++, Blocks.BEE_NEST,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.SOUTH,
                facingOnly, hiveActual, true);

        Set<String> campfire = Set.of("facing", "lit", "signal_fire", "waterlogged");
        assertComplexPlacement(
                level, player, origin, index++, Blocks.CAMPFIRE,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.WEST,
                campfire, campfire, true);
        PlacementResult waterCampfire = assertComplexPlacement(
                level, player, origin, index, Blocks.CAMPFIRE,
                Direction.UP, 1.0D, TargetKind.WATER, Direction.EAST,
                campfire, campfire, true);
        require(waterCampfire.actual().getValue(BlockStateProperties.WATERLOGGED),
                "campfire water oracle must produce waterlogged actual state");
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
        require(result.comparisonResult() == (expectedPossible
                        ? PlacementInspector.MATCH
                        : PlacementInspector.NONE),
                "trapdoor comparison mismatch for " + targetKind);
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
        require(result.comparisonResult() == PlacementInspector.MATCH,
                "axis comparison must match vanilla placement");
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
        require(result.comparisonResult() == PlacementInspector.MATCH,
                "slab comparison must match vanilla placement");
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

    private static PlacementResult assertComplexPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block block,
            Direction face,
            double clickY,
            TargetKind targetKind,
            Direction playerFacing,
            Set<String> predictedProperties,
            Set<String> actualProperties,
            boolean expectedPossible) {
        return assertComplexPlacement(
                level, player, origin, index, block, face, clickY, targetKind, playerFacing,
                predictedProperties, actualProperties, expectedPossible, SiteSetup.NONE);
    }

    private static PlacementResult assertComplexPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block block,
            Direction face,
            double clickY,
            TargetKind targetKind,
            Direction playerFacing,
            Set<String> predictedProperties,
            Set<String> actualProperties,
            boolean expectedPossible,
            SiteSetup setup) {
        PlacementResult result = previewAndPlace(
                level, player, origin, index, block, face, clickY,
                targetKind, false, playerFacing, setup);
        require((result.predicted() != null) == expectedPossible,
                "complex preview possibility mismatch for " + block + " / " + targetKind);
        require(result.actuallyPlaced() == expectedPossible,
                "vanilla complex placement possibility mismatch for " + block + " / " + targetKind);
        if (!expectedPossible) {
            require(result.comparisonResult() == PlacementInspector.NONE,
                    "impossible complex placement must not create a comparison");
            return result;
        }
        require(result.comparisonResult() == PlacementInspector.MATCH
                        || result.comparisonResult() == PlacementInspector.ADJUSTED,
                "complex comparison must match or explain a vanilla adjustment for " + block);
        require(propertyNames(CrosshairInspector.placementStateProperties(result.predicted()))
                        .equals(predictedProperties),
                "unexpected predicted property allowlist for " + block);
        require(propertyNames(CrosshairInspector.actualPlacementStateProperties(result.actual()))
                        .equals(actualProperties),
                "unexpected actual property allowlist for " + block);
        return result;
    }

    private static void assertStairNeighborShape(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Direction neighborSide,
            StairsShape expectedShape) {
        Set<String> properties = Set.of("facing", "half", "shape", "waterlogged");
        PlacementResult result = assertComplexPlacement(
                level, player, origin, index, Blocks.OAK_STAIRS,
                Direction.UP, 1.0D, TargetKind.SOLID, Direction.NORTH,
                properties, properties, true,
                (testLevel, target) -> testLevel.setBlockAndUpdate(
                        target.relative(neighborSide),
                        Blocks.OAK_STAIRS.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                                .setValue(BlockStateProperties.HALF, Half.BOTTOM)));
        require(result.actual().getValue(BlockStateProperties.STAIRS_SHAPE) == expectedShape,
                "stairs neighbor oracle did not produce " + expectedShape);
    }

    private static Set<String> propertyNames(List<String> properties) {
        Set<String> names = new HashSet<>();
        for (String property : properties) names.add(property.substring(0, property.indexOf('=')));
        return Set.copyOf(names);
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
        return previewAndPlace(
                level, player, origin, index, block, face, clickY,
                targetKind, sneaking, playerFacing, SiteSetup.NONE);
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
            Direction playerFacing,
            SiteSetup setup) {
        BlockPos clicked = testPosition(origin, index);
        clear(level, clicked);
        switch (targetKind) {
            case SOLID, BLOCKED -> level.setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState());
            case REPLACEABLE -> {
                level.setBlockAndUpdate(clicked.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(clicked, Blocks.SHORT_GRASS.defaultBlockState());
            }
            case WATER -> {
                level.setBlockAndUpdate(clicked.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(clicked, Blocks.WATER.defaultBlockState());
            }
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
            BlockPlaceContext previewContext = new BlockPlaceContext(
                    level, player, InteractionHand.MAIN_HAND, previewStack, hit);
            setup.prepare(level, previewContext.getClickedPos());
            BlockState predicted = CrosshairInspector.predictPlacementState(
                    level,
                    player,
                    InteractionHand.MAIN_HAND,
                    previewStack,
                    hit,
                    true);
            PlacementInspector comparison = new PlacementInspector();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
            long captureTick = level.getGameTime();
            boolean captured = comparison.capture(
                    level, player, InteractionHand.MAIN_HAND, hit, captureTick);

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
            comparison.observe(level, captureTick + PlacementInspector.SETTLE_TICKS);
            require(captured == (predicted != null), "comparison capture must follow prediction availability");
            return new PlacementResult(
                    predicted,
                    actual,
                    actual.getBlock() == block,
                    captured
                            ? PlacementInspector.compare(
                                    comparison.predictedState, comparison.actualState)
                            : PlacementInspector.NONE);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    private static BlockPos testPosition(BlockPos origin, int index) {
        return origin.offset((index % 8) * 3, 0, (index / 8) * 3);
    }

    private static void assertComparisonLifecycle(
            ServerLevel level,
            ServerLevel otherDimension,
            ServerPlayer player,
            BlockPos origin,
            int index) {
        require(otherDimension != null, "Nether test level is unavailable");
        BlockPos clicked = testPosition(origin, index);
        clear(level, clicked);
        level.setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState());
        BlockHitResult hit = new BlockHitResult(
                hitLocation(clicked, Direction.UP, 1.0D), Direction.UP, clicked, false);
        BlockPos target = clicked.above();
        long tick = level.getGameTime();

        PlacementInspector tracker = new PlacementInspector();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        require(!tracker.capture(level, player, InteractionHand.MAIN_HAND, hit, tick),
                "non-BlockItem must not create pending comparison state");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_LOG));
        require(!tracker.capture(level, player, InteractionHand.OFF_HAND, hit, tick),
                "offhand must not create pending comparison state");

        require(tracker.capture(level, player, InteractionHand.MAIN_HAND, hit, tick),
                "supported main-hand placement must be captured");
        require(tracker.predictedState.getBlock() == Blocks.OAK_LOG,
                "pending comparison must retain predicted block identity");
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        tracker.observe(level, tick + PlacementInspector.SETTLE_TICKS);
        require(PlacementInspector.compare(tracker.predictedState, tracker.actualState)
                        == PlacementInspector.UNAVAILABLE,
                "unrelated actual block must not consume pending comparison");

        tracker.resetSession(null);
        require(tracker.predictedState == null, "disconnect/session reset must clear pending state");
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        require(tracker.capture(level, player, InteractionHand.MAIN_HAND, hit, tick),
                "dimension test capture failed");
        tracker.observe(otherDimension, tick + PlacementInspector.SETTLE_TICKS);
        require(tracker.predictedState == null, "dimension change must clear pending state");

        require(tracker.capture(level, player, InteractionHand.MAIN_HAND, hit, tick),
                "timeout test capture failed");
        tracker.observe(level, tick + PlacementInspector.EXPIRY_TICKS + 1L);
        require(tracker.predictedState == null, "expired pending state must be cleared");

        require(tracker.capture(level, player, InteractionHand.MAIN_HAND, hit, tick),
                "adjusted comparison capture failed");
        level.setBlockAndUpdate(target, Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        tracker.observe(level, tick + PlacementInspector.SETTLE_TICKS);
        require(PlacementInspector.compare(tracker.predictedState, tracker.actualState)
                        == PlacementInspector.ADJUSTED,
                "same block with changed property must be adjusted");
        require(PlacementInspector.compare(tracker.predictedState, tracker.predictedState)
                        == PlacementInspector.MATCH,
                "equal states must match");
        require(PlacementInspector.compare(tracker.predictedState, Blocks.STONE.defaultBlockState())
                        == PlacementInspector.DIFFERENT,
                "different blocks must be modeled as different");
        require(PlacementInspector.compare(null, tracker.actualState)
                        == PlacementInspector.UNAVAILABLE,
                "missing prediction must be unavailable");
        tracker.clear();
    }

    private static void assertPatternConsistency(
            ServerLevel level,
            ServerLevel otherDimension,
            BlockPos origin) {
        require(otherDimension != null, "Nether test level is unavailable");
        PatternConsistencyInspector inspector = new PatternConsistencyInspector();
        BlockState reference = Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, false);
        level.setBlockAndUpdate(origin, reference);
        level.setBlockAndUpdate(origin.east(), reference);
        level.setBlockAndUpdate(origin.east(2), reference
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        level.setBlockAndUpdate(origin.east(3), reference
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.HALF, Half.TOP)
                .setValue(BlockStateProperties.OPEN, true));
        level.setBlockAndUpdate(origin.east(4), Blocks.STONE.defaultBlockState());

        require(inspector.selectReference(level, origin), "pattern reference selection failed");
        inspector.scanTick(level);
        require(inspector.scanCursor() == PatternConsistencyInspector.MAX_BLOCKS_PER_TICK,
                "pattern scan must enforce its per-tick block budget");
        completePatternScan(inspector, level);
        require(inspector.scanCursor() == 0, "pattern scan did not complete");
        require(inspector.compared() == 3
                        && inspector.matches() == 1
                        && inspector.mismatchTotal() == 2,
                "pattern scan must ignore different Block IDs and compare same-ID states");
        require(inspector.retainedMismatches() == 2,
                "pattern scan must retain each mismatch within capacity");
        require(inspector.mismatchSummary("orientation").equals("Facing  2")
                        && inspector.mismatchSummary("shape").equals("Half  1")
                        && inspector.mismatchSummary("interaction").equals("Open  1"),
                "pattern mismatch reasons must be deterministic");

        BlockPos denseOrigin = origin.offset(0, 0, 24);
        level.setBlockAndUpdate(denseOrigin, reference);
        int placed = 0;
        for (int x = -7; x <= 7 && placed < 70; x++) {
            for (int z = -7; z <= 7 && placed < 70; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlockAndUpdate(denseOrigin.offset(x, 0, z), reference
                        .setValue(BlockStateProperties.OPEN, true));
                placed++;
            }
        }
        require(inspector.selectReference(level, denseOrigin),
                "replacement pattern reference selection failed");
        require(inspector.compared() == 0,
                "new explicit reference must invalidate previous results");
        completePatternScan(inspector, level);
        require(inspector.mismatchTotal() == 70,
                "dense pattern scan must report every same-ID mismatch");
        require(inspector.retainedMismatches() == PatternConsistencyInspector.MAX_RETAINED_MISMATCHES,
                "pattern mismatch retention must stop at the hard result budget");

        level.setBlockAndUpdate(denseOrigin, Blocks.STONE.defaultBlockState());
        inspector.scanTick(level);
        require(!inspector.hasReference(),
                "replacement of the selected source block must clear the reference");
        level.setBlockAndUpdate(origin, reference);
        require(inspector.selectReference(level, origin), "dimension clear setup failed");
        inspector.scanTick(otherDimension);
        require(!inspector.hasReference(), "dimension change must clear pattern state");
        require(inspector.selectReference(level, origin), "disconnect clear setup failed");
        inspector.resetSession(null);
        require(!inspector.hasReference(), "disconnect must clear pattern state");
    }

    private static void completePatternScan(
            PatternConsistencyInspector inspector,
            ServerLevel level) {
        int ticks = (PatternConsistencyInspector.TOTAL_BLOCKS
                + PatternConsistencyInspector.MAX_BLOCKS_PER_TICK - 1)
                / PatternConsistencyInspector.MAX_BLOCKS_PER_TICK;
        for (int tick = 0; tick < ticks; tick++) inspector.scanTick(level);
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

    private record PlacementResult(
            BlockState predicted,
            BlockState actual,
            boolean actuallyPlaced,
            int comparisonResult) {
    }

    @FunctionalInterface
    private interface SiteSetup {
        SiteSetup NONE = (level, target) -> { };

        void prepare(ServerLevel level, BlockPos target);
    }

    private enum TargetKind {
        SOLID,
        REPLACEABLE,
        WATER,
        BLOCKED
    }
}
