package dev.chise.chisetweaks.gui;

import java.util.List;

/** Pure layout policy for the standalone settings launcher button. */
public final class ChiseTweaksLauncherLayout {
    public static final int BUTTON_WIDTH = 112;
    public static final int BUTTON_HEIGHT = 20;
    public static final int MARGIN = 6;
    public static final int GAP = 4;

    private ChiseTweaksLauncherLayout() {}

    public static Placement place(int screenWidth, int screenHeight, List<Bounds> occupied) {
        int safeWidth = Math.max(BUTTON_WIDTH + MARGIN * 2, screenWidth);
        int safeHeight = Math.max(BUTTON_HEIGHT + MARGIN * 2, screenHeight);

        int rightX = Math.max(MARGIN, safeWidth - BUTTON_WIDTH - MARGIN);
        Placement right = findColumn(rightX, safeHeight, occupied);
        if (right != null) return right;

        Placement left = findColumn(MARGIN, safeHeight, occupied);
        if (left != null) return left;

        return new Placement(rightX, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private static Placement findColumn(int x, int screenHeight, List<Bounds> occupied) {
        int y = MARGIN;
        while (y + BUTTON_HEIGHT <= screenHeight - MARGIN) {
            Bounds collision = firstCollision(x, y, occupied);
            if (collision == null) return new Placement(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            y = Math.max(y + 1, collision.bottom() + GAP);
        }
        return null;
    }

    private static Bounds firstCollision(int x, int y, List<Bounds> occupied) {
        if (occupied == null || occupied.isEmpty()) return null;
        Bounds candidate = new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        for (Bounds bounds : occupied) {
            if (bounds != null && candidate.overlaps(bounds)) return bounds;
        }
        return null;
    }

    public record Placement(int x, int y, int width, int height) {}

    public record Bounds(int x, int y, int width, int height) {
        public int right() {
            return x + Math.max(0, width);
        }

        public int bottom() {
            return y + Math.max(0, height);
        }

        public boolean overlaps(Bounds other) {
            return x < other.right()
                    && right() > other.x
                    && y < other.bottom()
                    && bottom() > other.y;
        }
    }
}
