package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** vanilla配置を変更せず、最後の1件だけを短時間観測して予測と比較する。 */
public final class PlacementComparisonTracker
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final long EXPIRY_TICKS = 100L;
    static final long SETTLE_TICKS = 2L;

    static final int NONE = -1;
    static final int UNAVAILABLE = 0;
    static final int MATCH = 1;
    static final int ADJUSTED = 2;
    static final int DIFFERENT = 3;

    private static volatile PlacementComparisonTracker active;

    private ResourceKey<Level> dimension;
    private BlockPos targetPos;
    BlockState predictedState;
    private BlockState stateBeforePlacement;
    BlockState actualState;
    private long creationTick;
    long revision;
    private boolean acceptingCallbacks;

    @Override
    public String getId() {
        return "placement-comparison";
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
            return;
        }
        observe(client.level, client.level.getGameTime());
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        acceptingCallbacks = false;
        clear();
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
        var stack = player.getItemInHand(hand);
        BlockState predicted = CrosshairInspector.predictPlacementState(
                level, player, hand, stack, hit, true);
        if (predicted == null) return false;
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, stack, hit);
        BlockPos target = context.getClickedPos();
        if (!level.isLoaded(target)) return false;

        dimension = level.dimension();
        targetPos = target;
        predictedState = predicted;
        stateBeforePlacement = level.getBlockState(target);
        actualState = null;
        creationTick = tick;
        revision++;
        return true;
    }

    void observe(Level level, long tick) {
        if (targetPos == null) return;
        if (level == null
                || !dimension.equals(level.dimension())
                || tick < creationTick
                || tick - creationTick > EXPIRY_TICKS) {
            clear();
            return;
        }
        if (actualState != null || tick - creationTick < SETTLE_TICKS || !level.isLoaded(targetPos)) return;
        BlockState observed = level.getBlockState(targetPos);
        if (observed == stateBeforePlacement) return;
        int observedResult = compare(predictedState, observed);
        if (observedResult == DIFFERENT || observedResult == UNAVAILABLE) return;
        actualState = observed;
        revision++;
    }

    static int compare(BlockState predicted, BlockState actual) {
        if (predicted == null || actual == null) return UNAVAILABLE;
        if (predicted.getBlock() != actual.getBlock()) return DIFFERENT;
        return predicted.equals(actual) ? MATCH : ADJUSTED;
    }

    static PlacementComparisonTracker activeAt(ResourceKey<Level> dimension, BlockPos pos) {
        PlacementComparisonTracker tracker = active;
        return tracker != null
                && tracker.targetPos != null
                && tracker.dimension.equals(dimension)
                && tracker.targetPos.equals(pos) ? tracker : null;
    }

    void clear() {
        if (targetPos == null) return;
        dimension = null;
        targetPos = null;
        predictedState = null;
        stateBeforePlacement = null;
        actualState = null;
        creationTick = 0L;
        revision++;
    }
}
