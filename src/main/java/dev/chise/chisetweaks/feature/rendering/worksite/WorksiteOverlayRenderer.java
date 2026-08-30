package dev.chise.chisetweaks.feature.rendering.worksite;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteOverlayDetailPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import dev.chise.chisetweaks.feature.rendering.SurfaceLineVisualGeometry;
import dev.chise.chisetweaks.feature.rendering.WorldLineGeometry;
import dev.chise.chisetweaks.runtime.ClientCallbackCircuitBreaker;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class WorksiteOverlayRenderer {
    private static final String RENDERER_REVISION = "surface-line-v10-mining-suppression";
    private static final int ACCENT_DARK = 0xFF4E3A8C;

    private final BooleanSupplier activeSupplier;
    private final Consumer<LevelRenderContext> guardedRender;
    private volatile List<WorksiteRenderTarget> targets = List.of();
    private List<WorksiteVisibleTarget> cachedVisibleTargets = List.of();
    private boolean rendererIdentityLogged;

    WorksiteOverlayRenderer(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        this.guardedRender = this::renderSafely;
    }

    void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
    }

    void updateTargets(List<WorksiteVisibleTarget> visibleTargets) {
        if (visibleTargets == null || visibleTargets.isEmpty()) {
            clear();
            return;
        }
        List<WorksiteVisibleTarget> snapshot = List.copyOf(visibleTargets);
        if (snapshot.equals(cachedVisibleTargets)) return;

        ArrayList<WorksiteRenderTarget> prepared = new ArrayList<>(snapshot.size());
        for (WorksiteVisibleTarget target : snapshot) {
            if (target != null) prepared.add(WorksiteRenderTarget.prepare(target));
        }
        cachedVisibleTargets = snapshot;
        targets = List.copyOf(prepared);
    }

    void clear() {
        cachedVisibleTargets = List.of();
        targets = List.of();
    }

    private void render(LevelRenderContext context) {
        if (!activeSupplier.getAsBoolean()) return;
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        if (!config.worksiteVisibilityWorldOverlay
                || ClientCallbackCircuitBreaker.isOpen(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER)) return;
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER,
                context,
                guardedRender);
    }

    private void renderSafely(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        List<WorksiteRenderTarget> snapshot = targets;
        if (snapshot.isEmpty() || client.player == null || client.level == null || client.screen != null) return;
        Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
        if (cameraPosition == null) return;

        if (!rendererIdentityLogged) {
            WorksiteRenderTarget first = snapshot.get(0);
            ChiseTweaksClient.LOGGER.info(
                    "Visual renderer {} active in ChiseTweaks {}; first target {} ({})",
                    RENDERER_REVISION,
                    ChiseTweaksMetadata.MOD_VERSION,
                    first.presentation().blockId(),
                    first.presentation().category());
            rendererIdentityLogged = true;
        }

        BlockPos attackedBlock = attackedBlockPosition(client);
        boolean attackPressed = client.options.keyAttack.isDown();

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.lines(),
                    (pose, vertices) -> {
                        long pulseFrame = System.nanoTime() / 150_000_000L;
                        for (WorksiteRenderTarget target : snapshot) {
                            Block liveBlock = liveBlock(client, target.position());
                            if (!shouldDrawTarget(target, liveBlock, attackPressed, attackedBlock)) continue;
                            drawTarget(vertices, pose, target, pulseFrame);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    static boolean shouldDrawTarget(
            WorksiteRenderTarget target,
            Block liveBlock,
            boolean attackPressed,
            BlockPos attackedBlock) {
        if (target == null || liveBlock == null || target.expectedBlock() == null
                || target.expectedBlock() != liveBlock) {
            return false;
        }
        return !attackPressed
                || attackedBlock == null
                || target.presentation().category() != BlockInspectionCategory.NETHER_PALETTE
                || !target.position().equals(attackedBlock);
    }

    private static Block liveBlock(Minecraft client, BlockPos position) {
        if (client.level == null
                || !client.level.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
            return null;
        }
        Block block = client.level.getBlockState(position).getBlock();
        return BuilderFocusVisibility.shouldHide(block) ? null : block;
    }

    private static BlockPos attackedBlockPosition(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return blockHit.getBlockPos();
    }

    private static void drawTarget(
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            PoseStack.Pose pose,
            WorksiteRenderTarget target,
            long pulseFrame) {
        VisualAssistanceStylePolicy.OverlayStyle style = target.style();
        int primary = style.argb();
        int phase = (int) Math.floorMod(pulseFrame + target.pulseSeed(), 8L);
        boolean compact = target.detail() == WorksiteOverlayDetailPolicy.Detail.COMPACT;

        BlockInspectionCategory category = target.presentation().category();
        if (category == BlockInspectionCategory.TECHNICAL_TRACE) {
            int stateColor = target.powered() ? adjustBrightness(primary, 26) : primary;
            int accent = target.powered()
                    ? adjustBrightness(primary, -42)
                    : adjustBrightness(primary, 30);
            if (compact) {
                SurfaceLineVisualGeometry.drawCompactFrame(
                        vertices, pose, target.position(), stateColor, 1.8f);
                if (target.tripwireHook()) {
                    WorldLineGeometry.drawOrientation(
                            vertices, pose, target.position(), target.orientation(), accent, 1.6f);
                }
            } else {
                SurfaceLineVisualGeometry.drawThreadSkin(
                        vertices,
                        pose,
                        target.position(),
                        target.presentation().details(),
                        stateColor,
                        accent,
                        4.6f);
                if (target.tripwireHook()) {
                    WorldLineGeometry.drawOrientation(
                            vertices, pose, target.position(), target.orientation(), accent, 2.3f);
                }
            }
        } else if (category == BlockInspectionCategory.HIDDEN_SURFACE) {
            int accent = adjustBrightness(primary, -44);
            if (compact) {
                SurfaceLineVisualGeometry.drawCompactFrame(
                        vertices, pose, target.position(), primary, 2.2f);
            } else {
                SurfaceLineVisualGeometry.drawHiddenSurfaceSkin(
                        vertices, pose, target.position(), primary, accent, 3.0f);
            }
        } else if (category == BlockInspectionCategory.NETHER_PALETTE) {
            if (compact) {
                SurfaceLineVisualGeometry.drawCompactFrame(
                        vertices, pose, target.position(), primary, 1.9f);
            } else {
                SurfaceLineVisualGeometry.drawNetherSkin(
                        vertices, pose, target.position(), primary, ACCENT_DARK, phase, 2.3f);
            }
        }
    }

    private static int adjustBrightness(int argb, int delta) {
        int alpha = argb >>> 24;
        int red = clampChannel((argb >>> 16 & 0xFF) + delta);
        int green = clampChannel((argb >>> 8 & 0xFF) + delta);
        int blue = clampChannel((argb & 0xFF) + delta);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
