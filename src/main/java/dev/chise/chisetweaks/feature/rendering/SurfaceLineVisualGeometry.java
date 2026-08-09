package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Public facade for Chise-owned surface-line geometry.
 *
 * <p>Generic primitives and placement-state geometry live in dedicated helpers so this class
 * stays focused on category-level visual composition. All hot-path helpers avoid temporary array
 * allocation; only vertices are emitted during a render frame.</p>
 */
public final class SurfaceLineVisualGeometry {
    private SurfaceLineVisualGeometry() {}

    public static void drawThreadSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            List<String> details,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        float cx = position.getX() + 0.5f;
        float cz = position.getZ() + 0.5f;
        float y = position.getY() + 0.095f;
        boolean north = has(details, "north=true");
        boolean east = has(details, "east=true");
        boolean south = has(details, "south=true");
        boolean west = has(details, "west=true");
        boolean connected = north || east || south || west;

        if (north) SurfaceLinePrimitives.doubleLine(
                vertices, pose, cx, y, cz, cx, y, position.getZ() + 0.02f,
                primaryArgb, accentArgb, lineWidth);
        if (east) SurfaceLinePrimitives.doubleLine(
                vertices, pose, cx, y, cz, position.getX() + 0.98f, y, cz,
                primaryArgb, accentArgb, lineWidth);
        if (south) SurfaceLinePrimitives.doubleLine(
                vertices, pose, cx, y, cz, cx, y, position.getZ() + 0.98f,
                primaryArgb, accentArgb, lineWidth);
        if (west) SurfaceLinePrimitives.doubleLine(
                vertices, pose, cx, y, cz, position.getX() + 0.02f, y, cz,
                primaryArgb, accentArgb, lineWidth);
        if (!connected) {
            SurfaceLinePrimitives.doubleLine(
                    vertices, pose,
                    position.getX() + 0.18f, y, cz,
                    position.getX() + 0.82f, y, cz,
                    primaryArgb, accentArgb, lineWidth);
            SurfaceLinePrimitives.doubleLine(
                    vertices, pose,
                    cx, y, position.getZ() + 0.18f,
                    cx, y, position.getZ() + 0.82f,
                    primaryArgb, accentArgb, lineWidth);
        }
    }

    public static void drawGlassSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        SurfaceLinePrimitives.drawFaceLattice(
                vertices, pose, position, accentArgb, Math.max(1.1f, lineWidth * 0.55f));
    }

    public static void drawHiddenSurfaceSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        SurfaceLinePrimitives.drawCornerBrackets(
                vertices, pose, position, accentArgb, Math.max(1.3f, lineWidth * 0.62f));
    }

    public static void drawMaterialSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int pulseArgb,
            int phase,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceVeins(
                vertices, pose, position, primaryArgb,
                Math.max(1.3f, lineWidth * 0.58f), phase);
        SurfaceLinePrimitives.drawFaceVeins(
                vertices, pose, position, pulseArgb,
                Math.max(1.0f, lineWidth * 0.38f), phase + 1);
    }

    public static void drawNetherSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            int phase,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceBands(
                vertices, pose, position, primaryArgb,
                Math.max(1.2f, lineWidth * 0.58f), phase);
        SurfaceLinePrimitives.drawCornerBrackets(
                vertices, pose, position, accentArgb,
                Math.max(1.0f, lineWidth * 0.42f));
    }

    public static void drawPlacementSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            String blockId,
            OrientationOverlayPolicy.Overlay overlay,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        PlacementGuideLineGeometry.draw(
                vertices,
                pose,
                position,
                blockId,
                overlay,
                primaryArgb,
                accentArgb,
                lineWidth);
    }

    /** Compact far-distance marker used to bound GPU vertex work. */
    public static void drawCompactFrame(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, position, argb, lineWidth);
    }

    /** Compact far-distance placement marker that retains the facing cue. */
    public static void drawCompactPlacementSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            OrientationOverlayPolicy.Overlay overlay,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        PlacementGuideLineGeometry.drawCompact(
                vertices, pose, position, overlay, primaryArgb, accentArgb, lineWidth);
    }

    private static boolean has(List<String> details, String token) {
        return details != null && details.contains(token);
    }
}
