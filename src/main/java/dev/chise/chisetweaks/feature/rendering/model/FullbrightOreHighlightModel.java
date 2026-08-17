package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Preserves the final base model and appends only Chise-owned ore overlay geometry.
 *
 * <p>Target classification is intentionally completed during model wrapping. Runtime emission does
 * not resolve block IDs, tags, compatibility maps, or ore heuristics. Compatibility edits that can
 * change the set of wrapped models request a coalesced resource-model reload instead.</p>
 */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final @Nullable Target target;
    private final ExtraModelKey<BlockStateModel> staticKey;
    private final ExtraModelKey<BlockStateModel> animatedKey;

    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile boolean staticOverlayResolved;
    private volatile boolean animatedOverlayResolved;

    FullbrightOreHighlightModel(
            BlockStateModel wrapped,
            @Nullable Target target,
            ExtraModelKey<BlockStateModel> staticKey,
            ExtraModelKey<BlockStateModel> animatedKey) {
        super(wrapped);
        this.target = target;
        this.staticKey = staticKey;
        this.animatedKey = animatedKey;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        super.emitQuads(emitter, level, pos, state, random, cullTest);
        if (!highlightEnabled()) return;

        boolean animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
        BlockStateModel overlay = overlayModel(animated);
        if (overlay == null) return;

        emitter.pushTransform(quad -> {
            FullbrightOverlayLighting.apply(quad);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } catch (RuntimeException failure) {
            warnOnce(EMIT_FAILURE_LOGGED, "Ore Highlight overlay emission", failure);
        } finally {
            emitter.popTransform();
        }
    }

    private boolean highlightEnabled() {
        boolean masterEnabled = FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue();
        if (!masterEnabled) return false;
        if (target == null) return true;
        return OreHighlightRuntimePolicy.shouldRender(
                true,
                LocalFeatureConfig.getInstance().visualTargetMask,
                target);
    }

    private static OreHighlightRuntimePolicy.Motion motion() {
        return OreHighlightRuntimePolicy.motion(
                LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled);
    }

    private @Nullable BlockStateModel overlayModel(boolean animated) {
        if (animated ? animatedOverlayResolved : staticOverlayResolved) {
            return animated ? animatedOverlay : staticOverlay;
        }
        ExtraModelKey<BlockStateModel> key = animated ? animatedKey : staticKey;
        BlockStateModel loaded = lookup(key);
        if (animated) {
            animatedOverlay = loaded;
            animatedOverlayResolved = true;
        } else {
            staticOverlay = loaded;
            staticOverlayResolved = true;
        }
        return loaded;
    }

    private static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Ore Highlight extra-model lookup", failure);
            return null;
        }
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null || !highlightEnabled()) return wrappedKey;
        return new GeometryKey(wrappedKey, target, motion());
    }

    private record GeometryKey(
            Object wrappedKey,
            @Nullable Target target,
            OreHighlightRuntimePolicy.Motion motion) {}

    private static void warnOnce(AtomicBoolean gate, String operation, RuntimeException failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }
}
