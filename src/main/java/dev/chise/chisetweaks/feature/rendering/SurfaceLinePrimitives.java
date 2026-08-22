package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

final class SurfaceLinePrimitives {
    static final float INSET = 0.018f;

    private SurfaceLinePrimitives() {}

    static void drawFaceFrame(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            int argb,
            float width) {
        drawBounds(vertices, pose,
                p.getX() + INSET, p.getY() + INSET, p.getZ() + INSET,
                p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                argb, width);
    }

    static void drawBounds(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int argb,
            float width) {
        line(vertices, pose, minX, minY, minZ, maxX, minY, minZ, argb, width);
        line(vertices, pose, maxX, minY, minZ, maxX, minY, maxZ, argb, width);
        line(vertices, pose, maxX, minY, maxZ, minX, minY, maxZ, argb, width);
        line(vertices, pose, minX, minY, maxZ, minX, minY, minZ, argb, width);
        line(vertices, pose, minX, maxY, minZ, maxX, maxY, minZ, argb, width);
        line(vertices, pose, maxX, maxY, minZ, maxX, maxY, maxZ, argb, width);
        line(vertices, pose, maxX, maxY, maxZ, minX, maxY, maxZ, argb, width);
        line(vertices, pose, minX, maxY, maxZ, minX, maxY, minZ, argb, width);
        line(vertices, pose, minX, minY, minZ, minX, maxY, minZ, argb, width);
        line(vertices, pose, maxX, minY, minZ, maxX, maxY, minZ, argb, width);
        line(vertices, pose, maxX, minY, maxZ, maxX, maxY, maxZ, argb, width);
        line(vertices, pose, minX, minY, maxZ, minX, maxY, maxZ, argb, width);
    }

    static void drawFaceLattice(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            int argb,
            float width) {
        float minX = p.getX() + INSET;
        float minY = p.getY() + INSET;
        float minZ = p.getZ() + INSET;
        float maxX = p.getX() + 1.0f - INSET;
        float maxY = p.getY() + 1.0f - INSET;
        float maxZ = p.getZ() + 1.0f - INSET;
        for (int i = 1; i <= 3; i++) {
            float f = i * 0.25f;
            float x = minX + (maxX - minX) * f;
            float y = minY + (maxY - minY) * f;
            float z = minZ + (maxZ - minZ) * f;
            line(vertices, pose, x, minY, minZ, x, maxY, minZ, argb, width);
            line(vertices, pose, x, minY, maxZ, x, maxY, maxZ, argb, width);
            line(vertices, pose, minX, y, minZ, maxX, y, minZ, argb, width);
            line(vertices, pose, minX, y, maxZ, maxX, y, maxZ, argb, width);
            line(vertices, pose, minX, minY, z, minX, maxY, z, argb, width);
            line(vertices, pose, maxX, minY, z, maxX, maxY, z, argb, width);
        }
    }

    static void drawCornerBrackets(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            int argb,
            float width) {
        float minX = p.getX() + 0.04f;
        float minY = p.getY() + 0.04f;
        float minZ = p.getZ() + 0.04f;
        float maxX = p.getX() + 0.96f;
        float maxY = p.getY() + 0.96f;
        float maxZ = p.getZ() + 0.96f;
        float segment = 0.22f;

        drawCornerBracketPlane(vertices, pose, minX, minY, maxX, maxY, minZ, segment, argb, width);
        drawCornerBracketPlane(vertices, pose, minX, minY, maxX, maxY, maxZ, segment, argb, width);
        line(vertices, pose, minX, minY, minZ, minX, minY, minZ + segment, argb, width);
        line(vertices, pose, minX, maxY, maxZ, minX, maxY, maxZ - segment, argb, width);
        line(vertices, pose, maxX, minY, minZ, maxX, minY, minZ + segment, argb, width);
        line(vertices, pose, maxX, maxY, maxZ, maxX, maxY, maxZ - segment, argb, width);
    }

    private static void drawCornerBracketPlane(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float z,
            float segment,
            int argb,
            float width) {
        line(vertices, pose, minX, minY, z, minX + segment, minY, z, argb, width);
        line(vertices, pose, minX, minY, z, minX, minY + segment, z, argb, width);
        line(vertices, pose, maxX, maxY, z, maxX - segment, maxY, z, argb, width);
        line(vertices, pose, maxX, maxY, z, maxX, maxY - segment, z, argb, width);
    }

    static void drawFaceVeins(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            int argb,
            float width,
            int phase) {
        float a = 0.18f + 0.08f * Math.floorMod(phase, 4);
        float b = 0.82f - 0.06f * Math.floorMod(phase + 1, 4);
        float x0 = p.getX() + INSET;
        float x1 = p.getX() + 1.0f - INSET;
        float y0 = p.getY() + INSET;
        float y1 = p.getY() + 1.0f - INSET;
        float z0 = p.getZ() + INSET;
        float z1 = p.getZ() + 1.0f - INSET;
        line(vertices, pose, p.getX() + a, y0, z0, p.getX() + b, y1, z0, argb, width);
        line(vertices, pose, p.getX() + b, y0, z1, p.getX() + a, y1, z1, argb, width);
        line(vertices, pose, x0, p.getY() + a, z0, x0, p.getY() + b, z1, argb, width);
        line(vertices, pose, x1, p.getY() + b, z0, x1, p.getY() + a, z1, argb, width);
        line(vertices, pose, x0, y1, p.getZ() + a, x1, y1, p.getZ() + b, argb, width);
        line(vertices, pose, x0, y0, p.getZ() + b, x1, y0, p.getZ() + a, argb, width);
    }

    static void drawFaceBands(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            int argb,
            float width,
            int phase) {
        float offset = 0.18f + 0.06f * Math.floorMod(phase, 3);
        for (int i = 0; i < 3; i++) {
            float f = Math.min(0.88f, offset + i * 0.27f);
            float x = p.getX() + f;
            float y = p.getY() + f;
            float z = p.getZ() + f;
            line(vertices, pose, x, p.getY() + INSET, p.getZ() + INSET,
                    x, p.getY() + 1.0f - INSET, p.getZ() + INSET, argb, width);
            line(vertices, pose, x, p.getY() + INSET, p.getZ() + 1.0f - INSET,
                    x, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET, argb, width);
            line(vertices, pose, p.getX() + INSET, y, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, y, p.getZ() + INSET, argb, width);
            line(vertices, pose, p.getX() + INSET, y, p.getZ() + 1.0f - INSET,
                    p.getX() + 1.0f - INSET, y, p.getZ() + 1.0f - INSET, argb, width);
            line(vertices, pose, p.getX() + INSET, p.getY() + INSET, z,
                    p.getX() + INSET, p.getY() + 1.0f - INSET, z, argb, width);
            line(vertices, pose, p.getX() + 1.0f - INSET, p.getY() + INSET, z,
                    p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, z, argb, width);
        }
    }

    static void drawHorizontalFaceCross(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            float y,
            int argb,
            float width) {
        line(vertices, pose, p.getX() + 0.14f, y, p.getZ() + 0.14f,
                p.getX() + 0.86f, y, p.getZ() + 0.86f, argb, width);
        line(vertices, pose, p.getX() + 0.14f, y, p.getZ() + 0.86f,
                p.getX() + 0.86f, y, p.getZ() + 0.14f, argb, width);
    }

    static void doubleLine(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float sx,
            float sy,
            float sz,
            float ex,
            float ey,
            float ez,
            int primary,
            int accent,
            float width) {
        line(vertices, pose, sx, sy, sz, ex, ey, ez, primary, width);
        line(vertices, pose, sx, sy + 0.009f, sz, ex, ey + 0.009f, ez,
                accent, Math.max(1.0f, width * 0.36f));
    }

    static void line(
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
