package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;

 
public final class WorldLineGeometry {
    private WorldLineGeometry() {}

    public static void drawOrientation(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            net.minecraft.core.BlockPos position,
            OrientationOverlayPolicy.Overlay overlay,
            int argb,
            float lineWidth) {
        if (overlay == null || overlay.empty()) return;
        float centerX = position.getX() + 0.5f;
        float centerY = position.getY() + 0.5f;
        float centerZ = position.getZ() + 0.5f;
        drawFacing(vertices, pose, centerX, centerY, centerZ, overlay.facing(), argb, lineWidth);
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
        if (facing == null) return;
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
