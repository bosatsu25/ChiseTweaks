package dev.chise.chisetweaks.feature.rendering.worksite;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import dev.chise.chisetweaks.feature.rendering.SurfaceLineVisualGeometry;
import dev.chise.chisetweaks.feature.rendering.WorldLineGeometry;
import dev.chise.chisetweaks.runtime.ClientCallbackCircuitBreaker;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns high-visibility world-space rendering for bounded worksite targets. */
final class WorksiteOverlayRenderer {
    private static final int[] PULSE_COLORS = {
            0xFF4E3A8C,
            0xFF5E4FA2,
            0xFF725AC1,
            0xFF7D6BDB,
            0xFF8F7AE5,
            0xFF9B7EDE,
            0xFFA68BFF,
            0xFFB29CFF
    };
    private static final int ACCENT_DARK = 0xFF4E3A8C;
    private static final int ACCENT_LIGHT = 0xFFB29CFF;
    private static final int THREAD_IDLE = 0xFF5E4FA2;
    private static final int THREAD_POWERED = 0xFFA68BFF;

    private final BooleanSupplier activeSupplier;
    private final Consumer<LevelRenderContext> guardedRender;
    private volatile List<WorksiteVisibleTarget> targets = List.of();

    WorksiteOverlayRenderer(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        this.guardedRender = this::renderSafely;
    }

    void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
    }

    void updateTargets(List<WorksiteVisibleTarget> targets) {
        this.targets = targets == null ? List.of() : targets;
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
        List<WorksiteVisibleTarget> snapshot = targets;
        if (snapshot.isEmpty() || client.player == null || client.level == null || client.screen != null) return;
        Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
        if (cameraPosition == null) return;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.lines(),
                    (pose, vertices) -> {
                        long pulseFrame = System.nanoTime() / 150_000_000L;
                        for (WorksiteVisibleTarget target : snapshot) {
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
            WorksiteVisibleTarget target,
            long pulseFrame) {
        VisualAssistanceStylePolicy.OverlayStyle style = target.style();
        int primary = style.argb();
        int phase = (int) Math.floorMod(pulseFrame + target.position().hashCode(), 8L);
        switch (target.presentation().category()) {
            case TECHNICAL_TRACE -> {
                boolean powered = target.presentation().details().contains("powered=true");
                int stateColor = powered ? THREAD_POWERED : THREAD_IDLE;
                int accent = powered ? ACCENT_DARK : ACCENT_LIGHT;
                SurfaceLineVisualGeometry.drawThreadSkin(
                        vertices, pose, target.position(), target.presentation().details(), stateColor, accent, 4.6f);
                if (target.presentation().blockId().endsWith("tripwire_hook")) {
                    WorldLineGeometry.drawOrientation(
                            vertices, pose, target.position(), target.orientation(), accent, 2.3f);
                }
            }
            case HIDDEN_SURFACE -> SurfaceLineVisualGeometry.drawHiddenSurfaceSkin(
                    vertices, pose, target.position(), primary, ACCENT_DARK, 3.0f);
            case GLASS_INSPECTION -> SurfaceLineVisualGeometry.drawGlassSkin(
                    vertices, pose, target.position(), primary, ACCENT_DARK, 2.8f);
            case PLACEMENT_GUIDE -> SurfaceLineVisualGeometry.drawPlacementSkin(
                    vertices,
                    pose,
                    target.position(),
                    target.presentation().blockId(),
                    target.orientation(),
                    primary,
                    ACCENT_LIGHT,
                    2.5f);
            case MATERIAL_HIGHLIGHT -> SurfaceLineVisualGeometry.drawMaterialSkin(
                    vertices, pose, target.position(), primary, pulseColor(target, pulseFrame), phase, 3.3f);
            case NETHER_PALETTE -> SurfaceLineVisualGeometry.drawNetherSkin(
                    vertices, pose, target.position(), primary, ACCENT_DARK, phase, 2.3f);
            case NONE -> { }
        }
    }

    private static int pulseColor(WorksiteVisibleTarget target, long pulseFrame) {
        int offset = Math.floorMod(target.position().hashCode(), PULSE_COLORS.length);
        int frame = (int) Math.floorMod(pulseFrame, PULSE_COLORS.length);
        return PULSE_COLORS[(frame + offset) % PULSE_COLORS.length];
    }
}
