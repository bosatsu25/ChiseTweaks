package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.SchematicPlacementComparisonPolicy;
import dev.chise.chisetweaks.integration.masa.LitematicaSchematicAccess;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vanilla placement prediction, short-lived actual-placement comparison and optional
 * Litematica schematic comparison share one bounded runtime component.
 */
public final class PlacementInspector
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final long EXPIRY_TICKS = 100L;
    static final long SETTLE_TICKS = 2L;

    static final int NONE = -1;
    static final int UNAVAILABLE = 0;
    static final int MATCH = 1;
    static final int ADJUSTED = 2;
    static final int DIFFERENT = 3;

    private static final SchematicSnapshot NO_SCHEMATIC =
            new SchematicSnapshot(null, 0, "", "");
    private static volatile PlacementInspector active;
    private static volatile SchematicSnapshot schematicSnapshot = NO_SCHEMATIC;

    private Level pendingLevel;
    private BlockPos targetPos;
    BlockState predictedState;
    BlockState actualState;
    private long creationTick;
    private boolean acceptingCallbacks;

    @Override
    public String getId() {
        return "placement-inspector";
    }

    @Override
    public void init() {
        active = this;
        acceptingCallbacks = true;
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.level == null) {
            clear();
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }
        observe(client.level, client.level.getGameTime());
        refreshSchematic(client);
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
        schematicSnapshot = NO_SCHEMATIC;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        acceptingCallbacks = false;
        clear();
        schematicSnapshot = NO_SCHEMATIC;
        if (active == this) active = null;
    }

    private InteractionResult onUseBlock(
            Player player,
            Level level,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!acceptingCallbacks
                || !level.isClientSide()
                || hand != InteractionHand.MAIN_HAND
                || player != Minecraft.getInstance().player) return InteractionResult.PASS;
        try {
            capture(level, player, hand, hit, level.getGameTime());
        } catch (RuntimeException | LinkageError ignored) {
            clear();
        }
        return InteractionResult.PASS;
    }

    boolean capture(
            Level level,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            long tick) {
        if (level == null || player == null || hand != InteractionHand.MAIN_HAND || hit == null) return false;
        PlacementProbe probe = placementProbe(
                level,
                player,
                hand,
                player.getItemInHand(hand),
                hit,
                true);
        if (probe == null || probe.predictedState() == null || !level.isLoaded(probe.targetPos())) {
            return false;
        }

        pendingLevel = level;
        targetPos = probe.targetPos();
        predictedState = probe.predictedState();
        actualState = null;
        creationTick = tick;
        return true;
    }

    void observe(Level level, long tick) {
        if (targetPos == null) return;
        if (level == null
                || pendingLevel != level
                || tick < creationTick
                || tick - creationTick > EXPIRY_TICKS) {
            clear();
            return;
        }
        if (actualState != null || tick - creationTick < SETTLE_TICKS || !level.isLoaded(targetPos)) return;
        BlockState observed = level.getBlockState(targetPos);
        int observedResult = compare(predictedState, observed);
        if (observedResult == DIFFERENT || observedResult == UNAVAILABLE) return;
        actualState = observed;
        InteractionHistory.recordPlacement(targetPos, predictedState, actualState);
    }

    private void refreshSchematic(Minecraft client) {
        if (!LocalFeatureConfig.getInstance().schematicPlacementInspectorEnabled
                || client.player == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        PlacementProbe probe = placementProbe(
                client.level,
                client.player,
                InteractionHand.MAIN_HAND,
                client.player.getMainHandItem(),
                hit,
                true);
        if (probe == null || probe.predictedState() == null || !client.level.isLoaded(probe.targetPos())) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        BlockState expected = LitematicaSchematicAccess.expectedState(probe.targetPos());
        int result = SchematicPlacementComparisonPolicy.compare(
                expected,
                probe.predictedState(),
                MasaIntegrationConfig.getInstance().pickRedirectMap);
        if (result == SchematicPlacementComparisonPolicy.NONE) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        schematicSnapshot = new SchematicSnapshot(
                probe.targetPos(),
                result,
                blockId(expected),
                blockId(probe.predictedState()));
    }

    static BlockState predictPlacementState(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hit,
            boolean enabled) {
        PlacementProbe probe = placementProbe(level, player, hand, stack, hit, enabled);
        return probe == null ? null : probe.predictedState();
    }

    static PlacementProbe placementProbe(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hit,
            boolean enabled) {
        if (!enabled
                || level == null
                || player == null
                || hand == null
                || stack == null
                || hit == null
                || !(stack.getItem() instanceof BlockItem item)
                || !supportsPlacementPreview(item.getBlock())) return null;
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, stack, hit);
        BlockState predicted = null;
        if (context.canPlace()) {
            BlockState state = item.getBlock().getStateForPlacement(context);
            if (state != null
                    && level.isUnobstructed(
                            state,
                            context.getClickedPos(),
                            CollisionContext.placementContext(player))) {
                predicted = stack.getOrDefault(
                        DataComponents.BLOCK_STATE,
                        BlockItemStateProperties.EMPTY).apply(state);
            }
        }
        return new PlacementProbe(context.getClickedPos().immutable(), predicted);
    }

    static boolean supportsPlacementPreview(Block block) {
        if (block instanceof TrapDoorBlock
                || block instanceof SlabBlock
                || block instanceof StairBlock
                || block instanceof GlazedTerracottaBlock
                || block instanceof FenceGateBlock
                || block instanceof GrindstoneBlock
                || block instanceof BeehiveBlock
                || block instanceof CampfireBlock) return true;
        BlockState state = block.defaultBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return state.is(BlockTags.LOGS)
                || id != null
                && "minecraft".equals(id.getNamespace())
                && id.getPath().endsWith("_froglight");
    }

    static List<String> placementStateProperties(BlockState state) {
        return placementStateProperties(state, false);
    }

    static List<String> actualPlacementStateProperties(BlockState state) {
        return placementStateProperties(state, true);
    }

    private static List<String> placementStateProperties(BlockState state, boolean actual) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        if (state.getBlock() instanceof TrapDoorBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            addPropertyIfPresent(state, BlockStateProperties.HALF, properties);
            addPropertyIfPresent(state, BlockStateProperties.OPEN, properties);
            addPropertyIfPresent(state, BlockStateProperties.POWERED, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else if (state.getBlock() instanceof SlabBlock) {
            addPropertyIfPresent(state, BlockStateProperties.SLAB_TYPE, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else if (state.getBlock() instanceof StairBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            addPropertyIfPresent(state, BlockStateProperties.HALF, properties);
            addPropertyIfPresent(state, BlockStateProperties.STAIRS_SHAPE, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else if (state.getBlock() instanceof GlazedTerracottaBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            addPropertyIfPresent(state, BlockStateProperties.OPEN, properties);
            addPropertyIfPresent(state, BlockStateProperties.POWERED, properties);
            addPropertyIfPresent(state, BlockStateProperties.IN_WALL, properties);
        } else if (state.getBlock() instanceof GrindstoneBlock) {
            addPropertyIfPresent(state, BlockStateProperties.ATTACH_FACE, properties);
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
        } else if (state.getBlock() instanceof BeehiveBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            if (actual) addPropertyIfPresent(state, BlockStateProperties.LEVEL_HONEY, properties);
        } else if (state.getBlock() instanceof CampfireBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            addPropertyIfPresent(state, BlockStateProperties.LIT, properties);
            addPropertyIfPresent(state, BlockStateProperties.SIGNAL_FIRE, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else {
            addPropertyIfPresent(state, BlockStateProperties.AXIS, properties);
        }
        return CrosshairSnapshotPolicy.formatStateProperties(properties);
    }

    private static <T extends Comparable<T>> void addPropertyIfPresent(
            BlockState state,
            Property<T> property,
            Map<String, String> properties) {
        if (state.hasProperty(property)) {
            properties.put(property.getName(), property.getName(state.getValue(property)));
        }
    }

    static int compare(BlockState predicted, BlockState actual) {
        if (predicted == null || actual == null) return UNAVAILABLE;
        if (predicted.getBlock() != actual.getBlock()) return DIFFERENT;
        return predicted.equals(actual) ? MATCH : ADJUSTED;
    }

    static PlacementInspector activeAt(Level level, BlockPos pos) {
        PlacementInspector inspector = active;
        return inspector != null
                && inspector.targetPos != null
                && inspector.pendingLevel == level
                && inspector.targetPos.equals(pos) ? inspector : null;
    }

    static SchematicSnapshot schematicSnapshot() {
        return schematicSnapshot;
    }

    static String schematicResultLabelAt(BlockPos pos) {
        SchematicSnapshot current = schematicSnapshot;
        return pos != null && pos.equals(current.targetPos())
                ? SchematicPlacementComparisonPolicy.label(current.result())
                : "";
    }

    void clear() {
        if (targetPos == null) return;
        pendingLevel = null;
        targetPos = null;
        predictedState = null;
        actualState = null;
        creationTick = 0L;
    }

    private static String blockId(BlockState state) {
        return state == null ? "" : String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    record PlacementProbe(
            BlockPos targetPos,
            BlockState predictedState) {}

    record SchematicSnapshot(
            BlockPos targetPos,
            int result,
            String expectedId,
            String predictedId) {

        boolean available() {
            return targetPos != null && result != SchematicPlacementComparisonPolicy.NONE;
        }

        String resultLabel() {
            return SchematicPlacementComparisonPolicy.label(result);
        }
    }
}
