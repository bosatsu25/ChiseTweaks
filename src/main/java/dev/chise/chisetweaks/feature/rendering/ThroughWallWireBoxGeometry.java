package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** 壁越し解析表示で共通利用する箱枠メッシュを生成する。 */
final class ThroughWallWireBoxGeometry {
    private static final Matrix4fc IDENTITY_POSE = new Matrix4f();

    private ThroughWallWireBoxGeometry() {
    }

    static void drawWireBox(
            BufferBuilder vertices,
            int blockX,
            int blockY,
            int blockZ,
            int argb,
            float thickness,
            float inset) {
        float minX = blockX + inset;
        float minY = blockY + inset;
        float minZ = blockZ + inset;
        float maxX = blockX + 1.0f - inset;
        float maxY = blockY + 1.0f - inset;
        float maxZ = blockZ + 1.0f - inset;
        float half = thickness * 0.5f;

        bar(vertices, minX, minY - half, minZ - half, maxX, minY + half, minZ + half, argb);
        bar(vertices, minX, minY - half, maxZ - half, maxX, minY + half, maxZ + half, argb);
        bar(vertices, minX, maxY - half, minZ - half, maxX, maxY + half, minZ + half, argb);
        bar(vertices, minX, maxY - half, maxZ - half, maxX, maxY + half, maxZ + half, argb);

        bar(vertices, minX - half, minY, minZ - half, minX + half, maxY, minZ + half, argb);
        bar(vertices, maxX - half, minY, minZ - half, maxX + half, maxY, minZ + half, argb);
        bar(vertices, minX - half, minY, maxZ - half, minX + half, maxY, maxZ + half, argb);
        bar(vertices, maxX - half, minY, maxZ - half, maxX + half, maxY, maxZ + half, argb);

        bar(vertices, minX - half, minY - half, minZ, minX + half, minY + half, maxZ, argb);
        bar(vertices, maxX - half, minY - half, minZ, maxX + half, minY + half, maxZ, argb);
        bar(vertices, minX - half, maxY - half, minZ, minX + half, maxY + half, maxZ, argb);
        bar(vertices, maxX - half, maxY - half, minZ, maxX + half, maxY + half, maxZ, argb);
    }

    private static void bar(
            BufferBuilder vertices,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int argb) {
        float red = ((argb >>> 16) & 0xFF) / 255.0f;
        float green = ((argb >>> 8) & 0xFF) / 255.0f;
        float blue = (argb & 0xFF) / 255.0f;
        float alpha = ((argb >>> 24) & 0xFF) / 255.0f;

        vertex(vertices, minX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, minX, minY, maxZ, red, green, blue, alpha);
    }

    private static void vertex(
            BufferBuilder vertices,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha) {
        vertices.addVertex(IDENTITY_POSE, x, y, z).setColor(red, green, blue, alpha);
    }
}
