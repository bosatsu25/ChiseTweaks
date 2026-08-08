package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Surface-anchored Chise-owned line geometry.
 *
 * <p>The scanner remains the source of visible targets, but these primitives move the visual
 * result from a generic box around the block to lines that sit on the block faces or approximate
 * the occupied model volume. This keeps the implementation independent from resource-pack assets
 * while making the final in-world cue behave more like a model/texture visibility aid.</p>
 */
public final class SurfaceLineVisualGeometry {
    private static final float INSET = 0.018f;

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

        if (north) doubleLine(vertices, pose, cx, y, cz, cx, y, position.getZ() + 0.02f,
                primaryArgb, accentArgb, lineWidth);
        if (east) doubleLine(vertices, pose, cx, y, cz, position.getX() + 0.98f, y, cz,
                primaryArgb, accentArgb, lineWidth);
        if (south) doubleLine(vertices, pose, cx, y, cz, cx, y, position.getZ() + 0.98f,
                primaryArgb, accentArgb, lineWidth);
        if (west) doubleLine(vertices, pose, cx, y, cz, position.getX() + 0.02f, y, cz,
                primaryArgb, accentArgb, lineWidth);
        if (!connected) {
            doubleLine(vertices, pose,
                    position.getX() + 0.18f, y, cz,
                    position.getX() + 0.82f, y, cz,
                    primaryArgb, accentArgb, lineWidth);
            doubleLine(vertices, pose,
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
        drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        drawFaceLattice(vertices, pose, position, accentArgb, Math.max(1.1f, lineWidth * 0.55f));
    }

    public static void drawHiddenSurfaceSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        drawCornerBrackets(vertices, pose, position, accentArgb, Math.max(1.3f, lineWidth * 0.62f));
    }

    public static void drawMaterialSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            int primaryArgb,
            int pulseArgb,
            int phase,
            float lineWidth) {
        drawFaceVeins(vertices, pose, position, primaryArgb,
                Math.max(1.3f, lineWidth * 0.58f), phase);
        drawFaceVeins(vertices, pose, position, pulseArgb,
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
        drawFaceBands(vertices, pose, position, primaryArgb,
                Math.max(1.2f, lineWidth * 0.58f), phase);
        drawCornerBrackets(vertices, pose, position, accentArgb,
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
        String id = blockId == null ? "" : blockId;
        OrientationOverlayPolicy.Overlay orientation =
                overlay == null ? OrientationOverlayPolicy.Overlay.EMPTY : overlay;

        if (id.endsWith("_slab")) {
            drawSlab(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_stairs")) {
            drawStairs(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_trapdoor")) {
            drawTrapdoor(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_fence_gate")) {
            drawFenceGate(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_froglight")
                || id.endsWith("_log")
                || id.endsWith("_wood")
                || id.endsWith("_stem")
                || id.endsWith("_hyphae")
                || id.endsWith("bamboo_block")) {
            drawAxisSkin(vertices, pose, position, orientation.axis(), primaryArgb, accentArgb, lineWidth);
            return;
        }

        drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        drawDirectionGlyph(vertices, pose, position, orientation, accentArgb,
                Math.max(1.5f, lineWidth * 0.72f));
    }

    private static void drawSlab(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
        float minY = p.getY() + (top ? 0.50f : INSET);
        float maxY = p.getY() + (top ? 1.0f - INSET : 0.50f);
        drawBounds(vertices, pose,
                p.getX() + INSET, minY, p.getZ() + INSET,
                p.getX() + 1.0f - INSET, maxY, p.getZ() + 1.0f - INSET,
                primary, width);
        float faceY = top ? minY + 0.008f : maxY - 0.008f;
        drawHorizontalFaceCross(vertices, pose, p, faceY, accent, Math.max(1.2f, width * 0.60f));
    }

    private static void drawStairs(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
        float low = p.getY() + INSET;
        float mid = p.getY() + 0.50f;
        float high = p.getY() + 1.0f - INSET;
        if (top) {
            drawBounds(vertices, pose,
                    p.getX() + INSET, mid, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, high, p.getZ() + 1.0f - INSET,
                    primary, width);
            drawDirectionalHalfBox(vertices, pose, p, overlay.facing(), low, mid, primary, width);
        } else {
            drawBounds(vertices, pose,
                    p.getX() + INSET, low, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, mid, p.getZ() + 1.0f - INSET,
                    primary, width);
            drawDirectionalHalfBox(vertices, pose, p, overlay.facing(), mid, high, primary, width);
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.3f, width * 0.62f));
    }

    private static void drawDirectionalHalfBox(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Facing facing,
            float minY,
            float maxY,
            int argb,
            float width) {
        float minX = p.getX() + INSET;
        float maxX = p.getX() + 1.0f - INSET;
        float minZ = p.getZ() + INSET;
        float maxZ = p.getZ() + 1.0f - INSET;
        if (facing == OrientationOverlayPolicy.Facing.EAST) minX = p.getX() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.WEST) maxX = p.getX() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.SOUTH) minZ = p.getZ() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.NORTH) maxZ = p.getZ() + 0.50f;
        drawBounds(vertices, pose, minX, minY, minZ, maxX, maxY, maxZ, argb, width);
    }

    private static void drawTrapdoor(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        float t = 0.09f;
        if (Boolean.TRUE.equals(overlay.open())) {
            switch (overlay.facing() == null ? OrientationOverlayPolicy.Facing.NORTH : overlay.facing()) {
                case NORTH -> drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + t,
                        primary, width);
                case SOUTH -> drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + 1.0f - t,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case WEST -> drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + t, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case EAST -> drawBounds(vertices, pose,
                        p.getX() + 1.0f - t, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case UP, DOWN -> drawFaceFrame(vertices, pose, p, primary, width);
            }
        } else {
            boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                    || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
            float minY = p.getY() + (top ? 1.0f - t : INSET);
            float maxY = p.getY() + (top ? 1.0f - INSET : t);
            drawBounds(vertices, pose,
                    p.getX() + INSET, minY, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, maxY, p.getZ() + 1.0f - INSET,
                    primary, width);
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.2f, width * 0.58f));
    }

    private static void drawFenceGate(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        float y0 = p.getY() + 0.24f;
        float y1 = p.getY() + 0.76f;
        boolean eastWest = overlay.facing() == OrientationOverlayPolicy.Facing.EAST
                || overlay.facing() == OrientationOverlayPolicy.Facing.WEST;
        if (Boolean.TRUE.equals(overlay.open())) {
            float a = 0.16f;
            float b = 0.84f;
            if (eastWest) {
                line(vertices, pose, p.getX() + 0.50f, y0, p.getZ() + a,
                        p.getX() + 0.50f, y1, p.getZ() + a, primary, width);
                line(vertices, pose, p.getX() + 0.50f, y0, p.getZ() + b,
                        p.getX() + 0.50f, y1, p.getZ() + b, primary, width);
            } else {
                line(vertices, pose, p.getX() + a, y0, p.getZ() + 0.50f,
                        p.getX() + a, y1, p.getZ() + 0.50f, primary, width);
                line(vertices, pose, p.getX() + b, y0, p.getZ() + 0.50f,
                        p.getX() + b, y1, p.getZ() + 0.50f, primary, width);
            }
        } else if (eastWest) {
            for (float y : new float[] {y0, y1}) {
                line(vertices, pose, p.getX() + INSET, y, p.getZ() + 0.50f,
                        p.getX() + 1.0f - INSET, y, p.getZ() + 0.50f, primary, width);
            }
        } else {
            for (float y : new float[] {y0, y1}) {
                line(vertices, pose, p.getX() + 0.50f, y, p.getZ() + INSET,
                        p.getX() + 0.50f, y, p.getZ() + 1.0f - INSET, primary, width);
            }
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.2f, width * 0.58f));
    }

    private static void drawAxisSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Axis axis,
            int primary,
            int accent,
            float width) {
        drawFaceFrame(vertices, pose, p, primary, width);
        float cx = p.getX() + 0.5f;
        float cy = p.getY() + 0.5f;
        float cz = p.getZ() + 0.5f;
        OrientationOverlayPolicy.Axis actual = axis == null ? OrientationOverlayPolicy.Axis.Y : axis;
        switch (actual) {
            case X -> {
                line(vertices, pose, p.getX() + INSET, cy, cz, p.getX() + 1.0f - INSET, cy, cz, accent, width);
                line(vertices, pose, p.getX() + INSET, cy - 0.16f, cz, p.getX() + 1.0f - INSET, cy - 0.16f, cz, accent, width * 0.62f);
                line(vertices, pose, p.getX() + INSET, cy + 0.16f, cz, p.getX() + 1.0f - INSET, cy + 0.16f, cz, accent, width * 0.62f);
            }
            case Y -> {
                line(vertices, pose, cx, p.getY() + INSET, cz, cx, p.getY() + 1.0f - INSET, cz, accent, width);
                line(vertices, pose, cx - 0.16f, p.getY() + INSET, cz, cx - 0.16f, p.getY() + 1.0f - INSET, cz, accent, width * 0.62f);
                line(vertices, pose, cx + 0.16f, p.getY() + INSET, cz, cx + 0.16f, p.getY() + 1.0f - INSET, cz, accent, width * 0.62f);
            }
            case Z -> {
                line(vertices, pose, cx, cy, p.getZ() + INSET, cx, cy, p.getZ() + 1.0f - INSET, accent, width);
                line(vertices, pose, cx, cy - 0.16f, p.getZ() + INSET, cx, cy - 0.16f, p.getZ() + 1.0f - INSET, accent, width * 0.62f);
                line(vertices, pose, cx, cy + 0.16f, p.getZ() + INSET, cx, cy + 0.16f, p.getZ() + 1.0f - INSET, accent, width * 0.62f);
            }
        }
    }

    private static void drawDirectionGlyph(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int argb,
            float width) {
        float cx = p.getX() + 0.5f;
        float cy = p.getY() + 0.5f;
        float cz = p.getZ() + 0.5f;
        OrientationOverlayPolicy.Facing facing = overlay.facing();
        if (facing == null) return;
        float dx = 0.0f;
        float dz = 0.0f;
        if (facing == OrientationOverlayPolicy.Facing.EAST) dx = 0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.WEST) dx = -0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.SOUTH) dz = 0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.NORTH) dz = -0.34f;
        else return;
        float ex = cx + dx;
        float ez = cz + dz;
        line(vertices, pose, cx, cy, cz, ex, cy, ez, argb, width);
        if (Math.abs(dx) > 0.01f) {
            line(vertices, pose, ex, cy, ez, ex - Math.signum(dx) * 0.12f, cy + 0.10f, ez, argb, width);
            line(vertices, pose, ex, cy, ez, ex - Math.signum(dx) * 0.12f, cy - 0.10f, ez, argb, width);
        } else {
            line(vertices, pose, ex, cy, ez, ex, cy + 0.10f, ez - Math.signum(dz) * 0.12f, argb, width);
            line(vertices, pose, ex, cy, ez, ex, cy - 0.10f, ez - Math.signum(dz) * 0.12f, argb, width);
        }
    }

    private static void drawFaceFrame(
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

    private static void drawBounds(
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

    private static void drawFaceLattice(
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
        for (float f : new float[] {0.25f, 0.50f, 0.75f}) {
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

    private static void drawCornerBrackets(
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
        float s = 0.22f;
        for (float z : new float[] {minZ, maxZ}) {
            line(vertices, pose, minX, minY, z, minX + s, minY, z, argb, width);
            line(vertices, pose, minX, minY, z, minX, minY + s, z, argb, width);
            line(vertices, pose, maxX, maxY, z, maxX - s, maxY, z, argb, width);
            line(vertices, pose, maxX, maxY, z, maxX, maxY - s, z, argb, width);
        }
        for (float x : new float[] {minX, maxX}) {
            line(vertices, pose, x, minY, minZ, x, minY, minZ + s, argb, width);
            line(vertices, pose, x, maxY, maxZ, x, maxY, maxZ - s, argb, width);
        }
    }

    private static void drawFaceVeins(
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

    private static void drawFaceBands(
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

    private static void drawHorizontalFaceCross(
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

    private static void doubleLine(
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

    private static boolean has(List<String> details, String token) {
        return details != null && details.contains(token);
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
