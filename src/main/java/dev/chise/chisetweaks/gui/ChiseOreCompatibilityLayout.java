package dev.chise.chisetweaks.gui;

 
public final class ChiseOreCompatibilityLayout {
    private static final int MAX_PANEL_WIDTH = 760;
    private static final int MIN_STANDARD_PANEL_WIDTH = 342;
    private static final int ROW_HEIGHT = 24;

    private ChiseOreCompatibilityLayout() {}

    public static Geometry calculate(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth >= 80 ? Math.min(12, Math.max(4, safeWidth / 24)) : 0;
        int panelWidth = Math.max(1, Math.min(MAX_PANEL_WIDTH, safeWidth - margin * 2));
        int panelX = Math.max(0, (safeWidth - panelWidth) / 2);
        boolean compact = panelWidth < MIN_STANDARD_PANEL_WIDTH;

        Rect idInput;
        Rect style;
        Rect add;
        int listTop;
        if (compact) {
            int inset = Math.min(8, Math.max(0, panelWidth / 12));
            int innerWidth = Math.max(1, panelWidth - inset * 2);
            int gap = innerWidth >= 16 ? 4 : 0;
            int addWidth = Math.max(1, Math.min(72, innerWidth / 3));
            int styleWidth = Math.max(1, innerWidth - gap - addWidth);
            idInput = new Rect(panelX + inset, 54, innerWidth, 20);
            style = new Rect(panelX + inset, 80, styleWidth, 20);
            add = new Rect(style.right() + gap, 80, addWidth, 20);
            listTop = 124;
        } else {
            int styleWidth = Math.min(150, Math.max(110, panelWidth / 4));
            int addWidth = 76;
            int inputWidth = Math.max(1, panelWidth - styleWidth - addWidth - 32);
            idInput = new Rect(panelX + 8, 70, inputWidth, 20);
            style = new Rect(panelX + 12 + inputWidth, 70, styleWidth, 20);
            add = new Rect(panelX + 16 + inputWidth + styleWidth, 70, addWidth, 20);
            listTop = 112;
        }

        int footerY = Math.max(0, safeHeight - 28);
        Footer footer = compact
                ? compactFooter(panelX, panelWidth, footerY)
                : standardFooter(panelX, panelWidth, footerY);
        int availableRows = Math.max(0, footerY - listTop - 4) / ROW_HEIGHT;
        int pageSize = Math.max(1, Math.min(10, availableRows));
        int removeWidth = compact
                ? Math.max(1, Math.min(54, Math.max(1, panelWidth / 4)))
                : 68;

        return new Geometry(
                new Rect(panelX, 34, panelWidth, Math.max(1, safeHeight - 68)),
                idInput,
                style,
                add,
                footer.previous(),
                footer.next(),
                footer.clear(),
                footer.back(),
                listTop,
                pageSize,
                removeWidth,
                compact);
    }

    private static Footer standardFooter(int panelX, int panelWidth, int y) {
        return new Footer(
                new Rect(panelX + 8, y, 58, 20),
                new Rect(panelX + 70, y, 58, 20),
                new Rect(panelX + 132, y, 122, 20),
                new Rect(panelX + panelWidth - 88, y, 80, 20));
    }

    private static Footer compactFooter(int panelX, int panelWidth, int y) {
        int inset = Math.min(8, Math.max(0, panelWidth / 12));
        int innerWidth = Math.max(1, panelWidth - inset * 2);
        int gap = innerWidth >= 20 ? 4 : 0;
        int usable = Math.max(1, innerWidth - gap * 3);
        int first = usable / 4;
        int second = usable / 4;
        int third = usable / 4;
        int fourth = usable - first - second - third;
        int x = panelX + inset;
        Rect previous = new Rect(x, y, first, 20);
        Rect next = new Rect(previous.right() + gap, y, second, 20);
        Rect clear = new Rect(next.right() + gap, y, third, 20);
        Rect back = new Rect(clear.right() + gap, y, fourth, 20);
        return new Footer(previous, next, clear, back);
    }

    private record Footer(Rect previous, Rect next, Rect clear, Rect back) {}

    public record Geometry(
            Rect panel,
            Rect idInput,
            Rect style,
            Rect add,
            Rect previous,
            Rect next,
            Rect clear,
            Rect back,
            int listTop,
            int pageSize,
            int removeWidth,
            boolean compact) {}

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(Rect other) {
            return other != null
                    && other.x >= x
                    && other.y >= y
                    && other.right() <= right()
                    && other.bottom() <= bottom();
        }
        public boolean overlaps(Rect other) {
            return other != null
                    && width > 0 && height > 0 && other.width > 0 && other.height > 0
                    && x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
    }
}
