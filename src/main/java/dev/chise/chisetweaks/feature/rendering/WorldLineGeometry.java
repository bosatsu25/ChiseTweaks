package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Shared line primitives for bounded Chise-owned world overlays. */
public final class WorldLineGeometry {
    private static final float BOX_INSET = 0.015f;

    private WorldLineGeometry() {
    }

    public static void drawBox(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        float minX = position.getX() + BOX_INSET;
        float minY = position.getY() + BOX_INSET;
        float minZ = position.getZ() + BOX_INSET;
        float maxX = position.getX() + 1.0f - BOX_INSET;
        float maxY = position.getY() + 1.0f - BOX_INSET;
        float maxZ = position.getZ() + 1.0f - BOX_INSET;

        line(vertices, pose, minX, minY, minZ, maxX, minY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, minZ, maxX, minY, maxZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, maxZ, minX, minY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, minY, maxZ, minX, minY, minZ, argb, lineWidth);

        line(vertices, pose, minX, maxY, minZ, maxX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, maxY, minZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, maxX, maxY, maxZ, minX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, maxY, maxZ, minX, maxY, minZ, argb, lineWidth);

        line(vertices, pose, minX, minY, minZ, minX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, minZ, maxX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, maxZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, minY, maxZ, minX, maxY, maxZ, argb, lineWidth);
    }

    public static void drawCross(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        float minX = position.getX() + 0.14f;
        float minY = position.getY() + 0.14f;
        float minZ = position.getZ() + 0.14f;
        float maxX = position.getX() + 0.86f;
        float maxY = position.getY() + 0.86f;
        float maxZ = position.getZ() + 0.86f;
        line(vertices, pose, minX, minY, minZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, maxY, minZ, maxX, minY, maxZ, argb, lineWidth);
    }

    public static void drawDiagonal(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        float minX = position.getX() + 0.08f;
        float minZ = position.getZ() + 0.08f;
        float maxX = position.getX() + 0.92f;
        float maxZ = position.getZ() + 0.92f;
        float y = position.getY() + 0.52f;
        line(vertices, pose, minX, y, minZ, maxX, y, maxZ, argb, lineWidth);
        line(vertices, pose, minX, y, maxZ, maxX, y, minZ, argb, lineWidth);
    }

    /** Thick state-coloured tripwire trace with a narrow white core for contrast. */
    public static void drawThreadSignal(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            List<String> details,
            int stateArgb,
            float lineWidth) {
        float centerX = position.getX() + 0.5f;
        float centerZ = position.getZ() + 0.5f;
        float y = position.getY() + 0.095f;
        boolean north = has(details, "north=true");
        boolean east = has(details, "east=true");
        boolean south = has(details, "south=true");
        boolean west = has(details, "west=true");
        boolean connected = north || east || south || west;

        if (north) signal(vertices, pose, centerX, y, centerZ, centerX, y, position.getZ() + 0.02f, stateArgb, lineWidth);
        if (east) signal(vertices, pose, centerX, y, centerZ, position.getX() + 0.98f, y, centerZ, stateArgb, lineWidth);
        if (south) signal(vertices, pose, centerX, y, centerZ, centerX, y, position.getZ() + 0.98f, stateArgb, lineWidth);
        if (west) signal(vertices, pose, centerX, y, centerZ, position.getX() + 0.02f, y, centerZ, stateArgb, lineWidth);
        if (!connected) {
            signal(vertices, pose,
                    position.getX() + 0.20f, y, centerZ,
                    position.getX() + 0.80f, y, centerZ,
                    stateArgb, lineWidth);
            signal(vertices, pose,
                    centerX, y, position.getZ() + 0.20f,
                    centerX, y, position.getZ() + 0.80f,
                    stateArgb, lineWidth);
        }
    }

    /** Dense high-contrast grid used instead of replacing stained-glass textures. */
    public static void drawGlassGrid(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        drawBox(vertices, pose, position, primaryArgb, lineWidth);
        drawFaceGrid(vertices, pose, position, accentArgb, Math.max(1.2f, lineWidth * 0.55f));
    }

    /** Surface-edge and diagonal hatch for visually ambiguous solid blocks. */
    public static void drawSurfaceHatch(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        drawBox(vertices, pose, position, primaryArgb, lineWidth);
        drawFaceDiagonals(vertices, pose, position, accentArgb, Math.max(1.2f, lineWidth * 0.60f));
    }

    /** High-contrast animated material marker without using upstream textures. */
    public static void drawMaterialPulse(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int pulseArgb,
            float lineWidth) {
        drawBox(vertices, pose, position, primaryArgb, lineWidth);
        drawCross(vertices, pose, position, pulseArgb, Math.max(2.0f, lineWidth * 0.80f));
        drawFaceDiagonals(vertices, pose, position, pulseArgb, Math.max(1.4f, lineWidth * 0.55f));
    }

    /** Low-information palette bands that make adjacent Nether materials easier to distinguish. */
    public static void drawNetherGrid(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        drawBox(vertices, pose, position, primaryArgb, lineWidth);
        float minX = position.getX() + 0.02f;
        float minY = position.getY() + 0.02f;
        float minZ = position.getZ() + 0.02f;
        float maxX = position.getX() + 0.98f;
        float maxY = position.getY() + 0.98f;
        float maxZ = position.getZ() + 0.98f;
        float w = Math.max(1.0f, lineWidth * 0.50f);
        for (float fraction : new float[] {0.25f, 0.50f, 0.75f}) {
            float y = minY + (maxY - minY) * fraction;
            line(vertices, pose, minX, y, minZ, maxX, y, minZ, accentArgb, w);
            line(vertices, pose, minX, y, maxZ, maxX, y, maxZ, accentArgb, w);
            line(vertices, pose, minX, y, minZ, minX, y, maxZ, accentArgb, w);
            line(vertices, pose, maxX, y, minZ, maxX, y, maxZ, accentArgb, w);
        }
    }

    public static void drawOrientation(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            OrientationOverlayPolicy.Overlay overlay,
            int argb,
            float lineWidth) {
        if (overlay == null || overlay.empty()) return;
        float centerX = position.getX() + 0.5f;
        float centerY = position.getY() + 0.5f;
        float centerZ = position.getZ() + 0.5f;
        if (overlay.facing() != null) {
            drawFacing(vertices, pose, centerX, centerY, centerZ, overlay.facing(), argb, lineWidth);
        }
        if (overlay.axis() != null) {
            drawAxis(vertices, pose, centerX, centerY, centerZ, overlay.axis(), argb, lineWidth);
        }
        if (overlay.half() != null) {
            drawHalf(vertices, pose, centerX, centerY, centerZ, overlay.half(), argb, lineWidth);
        }
        if (overlay.shape() != null) {
            drawShape(vertices, pose, centerX, centerY, centerZ, overlay.shape(), argb, lineWidth);
        }
        if (overlay.mountFace() != null) {
            drawMountFace(vertices, pose, centerX, centerY, centerZ, overlay.mountFace(), argb, lineWidth);
        }
        if (Boolean.TRUE.equals(overlay.open())) {
            drawOpenFlag(vertices, pose, centerX, centerY, centerZ, overlay.facing(), argb, lineWidth);
        }
    }

    private static void drawFaceGrid(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        float minX = position.getX() + 0.02f;
        float minY = position.getY() + 0.02f;
        float minZ = position.getZ() + 0.02f;
        float maxX = position.getX() + 0.98f;
        float maxY = position.getY() + 0.98f;
        float maxZ = position.getZ() + 0.98f;
        for (float fraction : new float[] {1.0f / 3.0f, 2.0f / 3.0f}) {
            float x = minX + (maxX - minX) * fraction;
            float y = minY + (maxY - minY) * fraction;
            float z = minZ + (maxZ - minZ) * fraction;
            line(vertices, pose, x, minY, minZ, x, maxY, minZ, argb, lineWidth);
            line(vertices, pose, x, minY, maxZ, x, maxY, maxZ, argb, lineWidth);
            line(vertices, pose, minX, y, minZ, maxX, y, minZ, argb, lineWidth);
            line(vertices, pose, minX, y, maxZ, maxX, y, maxZ, argb, lineWidth);
            line(vertices, pose, minX, minY, z, minX, maxY, z, argb, lineWidth);
            line(vertices, pose, maxX, minY, z, maxX, maxY, z, argb, lineWidth);
            line(vertices, pose, minX, minY, z, maxX, minY, z, argb, lineWidth);
            line(vertices, pose, minX, maxY, z, maxX, maxY, z, argb, lineWidth);
        }
    }

    private static void drawFaceDiagonals(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int argb,
            float lineWidth) {
        float minX = position.getX() + 0.04f;
        float minY = position.getY() + 0.04f;
        float minZ = position.getZ() + 0.04f;
        float maxX = position.getX() + 0.96f;
        float maxY = position.getY() + 0.96f;
        float maxZ = position.getZ() + 0.96f;

        line(vertices, pose, minX, minY, minZ, maxX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, minZ, minX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, minX, minY, maxZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, maxZ, minX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, minY, minZ, minX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, minY, maxZ, minX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, minZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, maxX, minY, maxZ, maxX, maxY, minZ, argb, lineWidth);
        line(vertices, pose, minX, maxY, minZ, maxX, maxY, maxZ, argb, lineWidth);
        line(vertices, pose, minX, maxY, maxZ, maxX, maxY, minZ, argb, lineWidth);
    }

    private static void signal(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float startX,
            float startY,
            float startZ,
            float endX,
            float endY,
            float endZ,
            int stateArgb,
            float lineWidth) {
        line(vertices, pose, startX, startY, startZ, endX, endY, endZ, stateArgb, lineWidth);
        line(vertices, pose, startX, startY + 0.008f, startZ, endX, endY + 0.008f, endZ,
                0xFFFFFFFF, Math.max(1.0f, lineWidth * 0.34f));
    }

    private static boolean has(List<String> details, String token) {
        return details != null && details.contains(token);
    }

    private static void drawFacing(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.Facing facing,
            int argb,
            float lineWidth) {
        float dx = 0.0f;
        float dy = 0.0f;
        float dz = 0.0f;
        switch (facing) {
            case DOWN -> dy = -1.0f;
            case UP -> dy = 1.0f;
            case NORTH -> dz = -1.0f;
            case SOUTH -> dz = 1.0f;
            case WEST -> dx = -1.0f;
            case EAST -> dx = 1.0f;
        }
        float endX = centerX + dx * 0.68f;
        float endY = centerY + dy * 0.68f;
        float endZ = centerZ + dz * 0.68f;
        line(vertices, pose, centerX, centerY, centerZ, endX, endY, endZ, argb, lineWidth);

        if (Math.abs(dy) > 0.5f) {
            line(vertices, pose, endX, endY, endZ,
                    endX - 0.14f, endY - dy * 0.18f, endZ, argb, lineWidth);
            line(vertices, pose, endX, endY, endZ,
                    endX + 0.14f, endY - dy * 0.18f, endZ, argb, lineWidth);
        } else if (Math.abs(dx) > 0.5f) {
            line(vertices, pose, endX, endY, endZ,
                    endX - dx * 0.18f, endY + 0.14f, endZ, argb, lineWidth);
            line(vertices, pose, endX, endY, endZ,
                    endX - dx * 0.18f, endY - 0.14f, endZ, argb, lineWidth);
        } else {
            line(vertices, pose, endX, endY, endZ,
                    endX, endY + 0.14f, endZ - dz * 0.18f, argb, lineWidth);
            line(vertices, pose, endX, endY, endZ,
                    endX, endY - 0.14f, endZ - dz * 0.18f, argb, lineWidth);
        }
    }

    private static void drawAxis(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.Axis axis,
            int argb,
            float lineWidth) {
        switch (axis) {
            case X -> line(vertices, pose,
                    centerX - 0.52f, centerY, centerZ,
                    centerX + 0.52f, centerY, centerZ, argb, lineWidth);
            case Y -> line(vertices, pose,
                    centerX, centerY - 0.52f, centerZ,
                    centerX, centerY + 0.52f, centerZ, argb, lineWidth);
            case Z -> line(vertices, pose,
                    centerX, centerY, centerZ - 0.52f,
                    centerX, centerY, centerZ + 0.52f, argb, lineWidth);
        }
    }

    private static void drawHalf(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.Half half,
            int argb,
            float lineWidth) {
        float y = switch (half) {
            case TOP, UPPER -> centerY + 0.31f;
            case BOTTOM, LOWER -> centerY - 0.31f;
        };
        line(vertices, pose, centerX - 0.34f, y, centerZ,
                centerX + 0.34f, y, centerZ, argb, lineWidth);
        line(vertices, pose, centerX, y, centerZ - 0.34f,
                centerX, y, centerZ + 0.34f, argb, lineWidth);
    }

    private static void drawShape(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.Shape shape,
            int argb,
            float lineWidth) {
        float y = centerY + 0.02f;
        switch (shape) {
            case STRAIGHT -> line(vertices, pose,
                    centerX - 0.28f, y, centerZ,
                    centerX + 0.28f, y, centerZ, argb, lineWidth);
            case INNER_LEFT, OUTER_LEFT -> {
                line(vertices, pose, centerX, y, centerZ,
                        centerX - 0.30f, y, centerZ, argb, lineWidth);
                line(vertices, pose, centerX, y, centerZ,
                        centerX, y, centerZ - 0.30f, argb, lineWidth);
            }
            case INNER_RIGHT, OUTER_RIGHT -> {
                line(vertices, pose, centerX, y, centerZ,
                        centerX + 0.30f, y, centerZ, argb, lineWidth);
                line(vertices, pose, centerX, y, centerZ,
                        centerX, y, centerZ + 0.30f, argb, lineWidth);
            }
        }
    }

    private static void drawMountFace(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.MountFace mountFace,
            int argb,
            float lineWidth) {
        switch (mountFace) {
            case FLOOR -> line(vertices, pose,
                    centerX - 0.24f, centerY - 0.38f, centerZ,
                    centerX + 0.24f, centerY - 0.38f, centerZ, argb, lineWidth);
            case CEILING -> line(vertices, pose,
                    centerX - 0.24f, centerY + 0.38f, centerZ,
                    centerX + 0.24f, centerY + 0.38f, centerZ, argb, lineWidth);
            case WALL -> line(vertices, pose,
                    centerX, centerY - 0.24f, centerZ - 0.38f,
                    centerX, centerY + 0.24f, centerZ - 0.38f, argb, lineWidth);
        }
    }

    private static void drawOpenFlag(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            OrientationOverlayPolicy.Facing facing,
            int argb,
            float lineWidth) {
        float offsetX = facing == OrientationOverlayPolicy.Facing.EAST ? 0.34f
                : facing == OrientationOverlayPolicy.Facing.WEST ? -0.34f : 0.0f;
        float offsetZ = facing == OrientationOverlayPolicy.Facing.SOUTH ? 0.34f
                : facing == OrientationOverlayPolicy.Facing.NORTH ? -0.34f : 0.0f;
        float x = centerX + offsetX;
        float z = centerZ + offsetZ;
        line(vertices, pose, x, centerY - 0.20f, z,
                x, centerY + 0.20f, z, argb, lineWidth);
        line(vertices, pose, x, centerY + 0.20f, z,
                x + 0.16f, centerY + 0.10f, z, argb, lineWidth);
    }

    private static void line(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float startX,
            float startY,
            float startZ,
            float endX,
            float endY,
            float endZ,
            int argb,
            float lineWidth) {
        float normalX = endX - startX;
        float normalY = endY - startY;
        float normalZ = endZ - startZ;
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 0.0001f) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }
        vertices.addVertex(pose, startX, startY, startZ)
                .setColor(argb)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(lineWidth);
        vertices.addVertex(pose, endX, endY, endZ)
                .setColor(argb)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(lineWidth);
    }
}
