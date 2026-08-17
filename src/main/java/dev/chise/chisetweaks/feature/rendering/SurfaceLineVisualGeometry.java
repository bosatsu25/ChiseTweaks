package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Category-level surface-line composition for retained worksite overlays. */
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

    /**
     * Draws an X-shaped two-color neon skin that follows the visual silhouette of kelp better than
     * a full block cube. Both colors are always present; the emphasis swaps slowly instead of
     * strobing so the effect remains readable during long underwater work sessions.
     */
    public static void drawKelpPartySkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int magentaArgb,
            int orangeArgb,
            int phase,
            float lineWidth) {
        boolean swapped = Math.floorMod(phase, 8) >= 4;
        int primary = swapped ? orangeArgb : magentaArgb;
        int accent = swapped ? magentaArgb : orangeArgb;
        float min = 0.22f;
        float max = 0.78f;
        float y0 = position.getY() + 0.06f;
        float y1 = position.getY() + 0.94f;
        float cx = position.getX() + 0.5f;
        float cz = position.getZ() + 0.5f;

        SurfaceLinePrimitives.doubleLine(
                vertices, pose,
                position.getX() + min, y0, cz,
                position.getX() + max, y1, cz,
                primary, accent, lineWidth);
        SurfaceLinePrimitives.doubleLine(
                vertices, pose,
                position.getX() + max, y0, cz,
                position.getX() + min, y1, cz,
                accent, primary, lineWidth);
        SurfaceLinePrimitives.doubleLine(
                vertices, pose,
                cx, y0, position.getZ() + min,
                cx, y1, position.getZ() + max,
                primary, accent, lineWidth);
        SurfaceLinePrimitives.doubleLine(
                vertices, pose,
                cx, y0, position.getZ() + max,
                cx, y1, position.getZ() + min,
                accent, primary, lineWidth);
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

    private static boolean has(List<String> details, String token) {
        return details != null && details.contains(token);
    }
}
