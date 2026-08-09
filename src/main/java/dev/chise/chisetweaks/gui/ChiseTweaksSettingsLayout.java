package dev.chise.chisetweaks.gui;

/**
 * Pure geometry policy for the standalone Chise settings screen.
 *
 * <p>This class deliberately has no Minecraft dependencies so responsive layout contracts can be
 * tested headlessly in CI. The rendered screen consumes the exact same geometry that the tests
 * validate.</p>
 */
public final class ChiseTweaksSettingsLayout {
    private static final int OUTER_MARGIN = 12;
    private static final int MAX_CONTENT_WIDTH = 1180;
    private static final int NAV_GAP = 5;
    private static final int NAV_COUNT = 4;
    private static final int BULK_GAP = 8;
    private static final int BULK_WIDTH = 134;

    private ChiseTweaksSettingsLayout() {}

    public static Geometry calculate(int screenWidth, int screenHeight, boolean categoryPage) {
        int safeWidth = Math.max(360, screenWidth);
        int safeHeight = Math.max(260, screenHeight);

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(336, safeWidth - OUTER_MARGIN * 2));
        int contentX = Math.max(OUTER_MARGIN, (safeWidth - contentWidth) / 2);

        // Chise is now a single standalone settings surface, so there is no mod-selector widget.
        Rect selector = Rect.EMPTY;

        int navY = 40;
        int navButtonWidth = clamp((contentWidth - NAV_GAP * (NAV_COUNT - 1)) / NAV_COUNT, 64, 112);
        int navTotalWidth = navButtonWidth * NAV_COUNT + NAV_GAP * (NAV_COUNT - 1);
        Rect navigation = new Rect(contentX, navY, navTotalWidth, 20);

        int searchY = 68;
        int searchWidth = categoryPage
                ? Math.max(150, contentWidth - BULK_WIDTH - BULK_GAP)
                : contentWidth;
        Rect search = new Rect(contentX, searchY, searchWidth, 20);
        Rect bulk = categoryPage
                ? new Rect(contentX + contentWidth - BULK_WIDTH, searchY, BULK_WIDTH, 20)
                : Rect.EMPTY;

        int footerY = safeHeight - 30;
        int panelY = 98;
        int panelHeight = Math.max(120, footerY - panelY - 10);
        Rect panel = new Rect(contentX, panelY, contentWidth, panelHeight);
        Rect footer = new Rect(contentX, footerY, contentWidth, 20);

        int controlWidth = clamp(contentWidth / 7, 88, 132);
        int panelPadding = 16;
        int nameX = panel.x() + panelPadding;
        int descriptionX = panel.x() + Math.max(190, Math.min(330, panel.width() / 3));
        int controlX = panel.right() - controlWidth - panelPadding;

        return new Geometry(
                new Rect(contentX, 0, contentWidth, safeHeight),
                selector,
                navigation,
                search,
                bulk,
                panel,
                footer,
                navButtonWidth,
                NAV_GAP,
                controlWidth,
                nameX,
                descriptionX,
                controlX,
                38,
                24);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Geometry(
            Rect content,
            Rect selector,
            Rect navigation,
            Rect search,
            Rect bulk,
            Rect panel,
            Rect footer,
            int navButtonWidth,
            int navGap,
            int controlWidth,
            int nameX,
            int descriptionX,
            int controlX,
            int rowHeight,
            int headerHeight) {

        public boolean categoryPage() { return !bulk.isEmpty(); }
        public int panelContentTop() { return panel.y() + 8; }
        public int panelContentBottom() { return panel.bottom() - 8; }
    }

    public record Rect(int x, int y, int width, int height) {
        public static final Rect EMPTY = new Rect(0, 0, 0, 0);
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean isEmpty() { return width <= 0 || height <= 0; }

        public boolean overlaps(Rect other) {
            if (other == null || isEmpty() || other.isEmpty()) return false;
            return x < other.right() && right() > other.x()
                    && y < other.bottom() && bottom() > other.y();
        }

        public boolean contains(Rect other) {
            if (other == null) return false;
            return other.x() >= x && other.y() >= y
                    && other.right() <= right() && other.bottom() <= bottom();
        }
    }
}
