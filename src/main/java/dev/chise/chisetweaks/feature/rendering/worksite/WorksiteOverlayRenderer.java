package dev.chise.chisetweaks.feature.rendering.worksite;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
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
            0xFFFF3B30,
            0xFFFF9500,
            0xFFFFE14A,
            0xFF71E35B,
            0xFF50E3E6,
            0xFF5A7DFF,
            0xFFC66BFF
    };

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
                        long pulseFrame = System.nanoTime() / 140_000_000L;
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
        switch (target.presentation().category()) {
            case TECHNICAL_TRACE -> {
                int stateColor = target.presentation().details().contains("powered=true")
                        ? 0xFFFF3B30
                        : 0xFF46FF6A;
                WorldLineGeometry.drawThreadSignal(
                        vertices, pose, target.position(), target.presentation().details(), stateColor, 4.6f);
                if (target.presentation().blockId().endsWith("tripwire_hook")) {
                    WorldLineGeometry.drawBox(vertices, pose, target.position(), stateColor, 2.4f);
                    WorldLineGeometry.drawOrientation(
                            vertices, pose, target.position(), target.orientation(), 0xFFFFFFFF, 2.2f);
                }
            }
            case HIDDEN_SURFACE -> WorldLineGeometry.drawSurfaceHatch(
                    vertices, pose, target.position(), primary, 0xE6FFFFFF, 3.0f);
            case GLASS_INSPECTION -> WorldLineGeometry.drawGlassGrid(
                    vertices, pose, target.position(), glassColor(target.presentation().blockId()), 0xE6FFFFFF, 3.2f);
            case PLACEMENT_GUIDE -> {
                WorldLineGeometry.drawBox(vertices, pose, target.position(), primary, 2.2f);
                WorldLineGeometry.drawOrientation(
                        vertices, pose, target.position(), target.orientation(), 0xFFFFFFFF, 3.0f);
            }
            case MATERIAL_HIGHLIGHT -> WorldLineGeometry.drawMaterialPulse(
                    vertices, pose, target.position(), primary, pulseColor(target, pulseFrame), 3.4f);
            case NETHER_PALETTE -> WorldLineGeometry.drawNetherGrid(
                    vertices, pose, target.position(), primary, 0xBFF7E8DC, 2.4f);
            case NONE -> { }
        }
    }

    private static int pulseColor(WorksiteVisibleTarget target, long pulseFrame) {
        int offset = Math.floorMod(target.position().hashCode(), PULSE_COLORS.length);
        int frame = (int) Math.floorMod(pulseFrame, PULSE_COLORS.length);
        return PULSE_COLORS[(frame + offset) % PULSE_COLORS.length];
    }

    private static int glassColor(String blockId) {
        String id = blockId == null ? "" : blockId;
        if (id.equals("minecraft:glass") || id.equals("minecraft:glass_pane")) return 0xFFE8F7FF;
        if (id.equals("minecraft:tinted_glass")) return 0xFF655E78;
        String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        String color = path
                .replace("_stained_glass_pane", "")
                .replace("_stained_glass", "");
        return switch (color) {
            case "white" -> 0xFFF0F0F0;
            case "orange" -> 0xFFF2A65A;
            case "magenta" -> 0xFFD66BD6;
            case "light_blue" -> 0xFF79C8F2;
            case "yellow" -> 0xFFF4E45C;
            case "lime" -> 0xFF8FD14F;
            case "pink" -> 0xFFF29AB2;
            case "gray" -> 0xFF777C83;
            case "light_gray" -> 0xFFB8BDC3;
            case "cyan" -> 0xFF48B8C4;
            case "purple" -> 0xFF9365C8;
            case "blue" -> 0xFF4D6FD6;
            case "brown" -> 0xFF8A5A3C;
            case "green" -> 0xFF4E9B56;
            case "red" -> 0xFFE05252;
            case "black" -> 0xFF404047;
            default -> 0xFFD8D8D8;
        };
    }
}
