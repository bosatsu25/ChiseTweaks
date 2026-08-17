package dev.chise.chisetweaks.feature.rendering.worksite;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteOverlayDetailPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import dev.chise.chisetweaks.feature.rendering.SurfaceLineVisualGeometry;
import dev.chise.chisetweaks.feature.rendering.WorldLineGeometry;
import dev.chise.chisetweaks.runtime.ClientCallbackCircuitBreaker;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns high-visibility world-space rendering for retained bounded worksite targets. */
final class WorksiteOverlayRenderer {
    private static final String RENDERER_REVISION = "surface-line-v7-worksite-only";
    private static final int ACCENT_DARK = 0xFF4E3A8C;
    private static final int ACCENT_LIGHT = 0xFFB29CFF;
    private static final int THREAD_IDLE = 0xFF5E4FA2;
    private static final int THREAD_POWERED = 0xFFA68BFF;

    private final BooleanSupplier activeSupplier;
    private final Consumer<LevelRenderContext> guardedRender;
    private volatile List<WorksiteRenderTarget> targets = List.of();
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
            targets = List.of();
            return;
        }
        ArrayList<WorksiteRenderTarget> prepared = new ArrayList<>(visibleTargets.size());
        for (WorksiteVisibleTarget target : visibleTargets) {
            if (target != null) prepared.add(WorksiteRenderTarget.prepare(target));
        }
        targets = List.copyOf(prepared);
    }

    void clear() {
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
                            drawTarget(vertices, pose, target, pulseFrame);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
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

        switch (target.presentation().category()) {
            case TECHNICAL_TRACE -> {
                int stateColor = target.powered() ? THREAD_POWERED : THREAD_IDLE;
                int accent = target.powered() ? ACCENT_DARK : ACCENT_LIGHT;
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
            }
            case HIDDEN_SURFACE -> {
                if (compact) {
                    SurfaceLineVisualGeometry.drawCompactFrame(
                            vertices, pose, target.position(), primary, 2.2f);
                } else {
                    SurfaceLineVisualGeometry.drawHiddenSurfaceSkin(
                            vertices, pose, target.position(), primary, ACCENT_DARK, 3.0f);
                }
            }
            case GLASS_INSPECTION -> {
                if (compact) {
                    SurfaceLineVisualGeometry.drawCompactFrame(
                            vertices, pose, target.position(), primary, 2.0f);
                } else {
                    SurfaceLineVisualGeometry.drawGlassSkin(
                            vertices, pose, target.position(), primary, ACCENT_DARK, 2.8f);
                }
            }
            case MATERIAL_HIGHLIGHT -> { }
            case NETHER_PALETTE -> {
                if (compact) {
                    SurfaceLineVisualGeometry.drawCompactFrame(
                            vertices, pose, target.position(), primary, 1.9f);
                } else {
                    SurfaceLineVisualGeometry.drawNetherSkin(
                            vertices, pose, target.position(), primary, ACCENT_DARK, phase, 2.3f);
                }
            }
            case NONE -> { }
        }
    }
}
