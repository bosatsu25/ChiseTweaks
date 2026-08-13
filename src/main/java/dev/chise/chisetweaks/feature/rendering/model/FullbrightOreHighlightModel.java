package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
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

/** Preserves the final base model and appends only Chise-owned ore overlay geometry. */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final @Nullable Target fixedTarget;
    private final @Nullable ExtraModelKey<BlockStateModel> fixedStaticKey;
    private final @Nullable ExtraModelKey<BlockStateModel> fixedAnimatedKey;
    private final boolean dynamicModded;
    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile boolean staticOverlayResolved;
    private volatile boolean animatedOverlayResolved;

    FullbrightOreHighlightModel(
            BlockStateModel wrapped,
            Target target,
            ExtraModelKey<BlockStateModel> staticOverlayKey,
            ExtraModelKey<BlockStateModel> animatedOverlayKey) {
        super(wrapped);
        this.fixedTarget = target;
        this.fixedStaticKey = staticOverlayKey;
        this.fixedAnimatedKey = animatedOverlayKey;
        this.dynamicModded = false;
    }

    FullbrightOreHighlightModel(BlockStateModel wrapped) {
        super(wrapped);
        this.fixedTarget = null;
        this.fixedStaticKey = null;
        this.fixedAnimatedKey = null;
        this.dynamicModded = true;
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

        boolean animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
        BlockStateModel overlay;
        if (dynamicModded) {
            OreHighlightResolver.Resolved resolved = activeModded(state);
            if (resolved == null || resolved.style() == null) return;
            overlay = dynamicOverlayModel(resolved.style(), animated);
        } else {
            if (!fixedHighlightEnabled()) return;
            overlay = fixedOverlayModel(animated);
        }
        if (overlay == null) return;

        emitter.pushTransform(quad -> {
            applyShaderInvariantHighlightLighting(quad);
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

    private static void applyShaderInvariantHighlightLighting(MutableQuadView quad) {
        quad.emissive(true);
        quad.diffuseShade(false);
        quad.ambientOcclusion(TriState.FALSE);
    }

    private boolean fixedHighlightEnabled() {
        if (fixedTarget == null) return false;
        return OreHighlightRuntimePolicy.shouldRender(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                LocalFeatureConfig.getInstance().visualTargetMask,
                fixedTarget);
    }

    private static @Nullable OreHighlightResolver.Resolved activeModded(BlockState state) {
        if (!FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue()) return null;
        OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(state);
        return resolved != null && resolved.target() == null ? resolved : null;
    }

    private static OreHighlightRuntimePolicy.Motion motion() {
        return OreHighlightRuntimePolicy.motion(
                LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled);
    }

    private @Nullable BlockStateModel fixedOverlayModel(boolean animated) {
        if (animated ? animatedOverlayResolved : staticOverlayResolved) {
            return animated ? animatedOverlay : staticOverlay;
        }
        ExtraModelKey<BlockStateModel> key = animated ? fixedAnimatedKey : fixedStaticKey;
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

    private static @Nullable BlockStateModel dynamicOverlayModel(OreHighlightStyle style, boolean animated) {
        OreHighlightOverlayCatalog.OverlayModels models = OreHighlightOverlayCatalog.forStyle(style);
        if (models == null) return null;
        return lookup(animated ? models.animatedKey() : models.staticKey());
    }

    private static @Nullable BlockStateModel lookup(@Nullable ExtraModelKey<BlockStateModel> key) {
        if (key == null) return null;
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Ore Highlight extra-model lookup", failure);
            return null;
        }
    }

    private static void warnOnce(AtomicBoolean gate, String operation, RuntimeException failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null) return null;
        if (!dynamicModded) {
            boolean enabled = fixedHighlightEnabled();
            return new FixedGeometryKey(
                    wrappedKey,
                    fixedTarget,
                    enabled,
                    enabled ? motion() : OreHighlightRuntimePolicy.Motion.STATIC);
        }

        OreHighlightResolver.Resolved resolved = activeModded(state);
        boolean enabled = resolved != null && resolved.style() != null;
        return new DynamicGeometryKey(
                wrappedKey,
                enabled ? resolved.style() : null,
                enabled,
                enabled ? motion() : OreHighlightRuntimePolicy.Motion.STATIC,
                OreHighlightExternalRegistry.revision());
    }

    private record FixedGeometryKey(
            Object wrappedKey,
            @Nullable Target target,
            boolean enabled,
            OreHighlightRuntimePolicy.Motion motion) {}

    private record DynamicGeometryKey(
            Object wrappedKey,
            @Nullable OreHighlightStyle style,
            boolean enabled,
            OreHighlightRuntimePolicy.Motion motion,
            long registryRevision) {}
}
