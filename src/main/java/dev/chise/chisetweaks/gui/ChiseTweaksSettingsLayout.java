package dev.chise.chisetweaks.gui;

import java.util.ArrayList;
import java.util.List;

public final class ChiseTweaksSettingsLayout {
    private static final int OUTER_MARGIN = 12;
    private static final int MAX_CONTENT_WIDTH = 960;
    private static final int TAB_Y = 30;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int TAB_COUNT = 6;
    private static final int PANEL_Y = 56;
    private static final int CONTROL_GAP = 6;
    private static final int TEXT_CONTROL_GAP = 12;
    private static final int SCROLLBAR_GUTTER = 24;
    private static final int INTEGER_CONTROL_WIDTH = 110;
    private static final int FOOTER_GAP = 6;
    private static final int CONTEXT_WIDTH = 132;
    private static final int DONE_WIDTH = 132;

    private ChiseTweaksSettingsLayout() {}

    public static Geometry calculate(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth >= 80 ? Math.min(OUTER_MARGIN, Math.max(4, safeWidth / 24)) : 0;

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(1, safeWidth - margin * 2));
        int contentX = Math.max(0, (safeWidth - contentWidth) / 2);
        Rect content = new Rect(contentX, 0, contentWidth, safeHeight);

        List<Rect> tabs = tabButtons(content);
        int footerY = Math.max(0, safeHeight - 26);
        int panelHeight = Math.max(1, footerY - PANEL_Y - 6);
        Rect panel = new Rect(contentX, PANEL_Y, contentWidth, panelHeight);
        Rect footer = new Rect(contentX, footerY, contentWidth, Math.min(20, safeHeight - footerY));
        Rect contextButton;
        Rect doneButton;
        int preferredFooterWidth = CONTEXT_WIDTH + FOOTER_GAP + DONE_WIDTH;
        if (footer.width() >= preferredFooterWidth) {
            doneButton = new Rect(footer.right() - DONE_WIDTH, footer.y(), DONE_WIDTH, footer.height());
            contextButton = new Rect(doneButton.x() - FOOTER_GAP - CONTEXT_WIDTH,
                    footer.y(), CONTEXT_WIDTH, footer.height());
        } else {
            int usableFooterWidth = Math.max(2, footer.width() - FOOTER_GAP);
            int contextWidth = usableFooterWidth / 2;
            contextButton = new Rect(footer.x(), footer.y(), contextWidth, footer.height());
            doneButton = new Rect(contextButton.right() + FOOTER_GAP, footer.y(),
                    usableFooterWidth - contextWidth, footer.height());
        }

        int panelPadding = clamp(contentWidth / 24, 6, 16);
        int toggleWidth = clamp(contentWidth / 7, 64, 84);
        int actionWidth = clamp(contentWidth / 6, 72, 96);
        int toggleX = Math.max(panel.x() + panelPadding,
                panel.right() - panelPadding - toggleWidth);
        int actionX = Math.max(panel.x() + panelPadding,
                toggleX - CONTROL_GAP - actionWidth);
        int integerX = Math.max(panel.x() + panelPadding,
                panel.right() - panelPadding - INTEGER_CONTROL_WIDTH);

        int nameX = panel.x() + panelPadding;
        int nameWidth = Math.max(1, actionX - TEXT_CONTROL_GAP - nameX);
        int rowHeight = contentWidth < 480 ? 34 : 30;
        int infoRowHeight = contentWidth < 480 ? 48 : 42;
        int headerHeight = 24;

        return new Geometry(
                content,
                tabs,
                panel,
                footer,
                contextButton,
                doneButton,
                toggleWidth,
                actionWidth,
                nameX,
                nameWidth,
                actionX,
                toggleX,
                integerX,
                rowHeight,
                infoRowHeight,
                headerHeight);
    }

    private static List<Rect> tabButtons(Rect content) {
        int usable = Math.max(TAB_COUNT, content.width() - TAB_GAP * (TAB_COUNT - 1));
        int baseWidth = Math.max(1, usable / TAB_COUNT);
        int remainder = Math.max(0, usable - baseWidth * TAB_COUNT);
        ArrayList<Rect> result = new ArrayList<>(TAB_COUNT);
        int x = content.x();
        for (int index = 0; index < TAB_COUNT; index++) {
            int width = baseWidth + (index < remainder ? 1 : 0);
            result.add(new Rect(x, TAB_Y, width, TAB_HEIGHT));
            x += width + TAB_GAP;
        }
        return List.copyOf(result);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Geometry(
            Rect content,
            List<Rect> tabs,
            Rect panel,
            Rect footer,
            Rect contextButton,
            Rect doneButton,
            int toggleWidth,
            int actionWidth,
            int nameX,
            int nameWidth,
            int actionX,
            int toggleX,
            int integerX,
            int rowHeight,
            int infoRowHeight,
            int headerHeight) {

        public int panelContentTop() { return panel.y() + Math.min(8, Math.max(0, panel.height() / 4)); }
        public int panelContentBottom() { return panel.bottom() - Math.min(8, Math.max(0, panel.height() / 4)); }
        public int infoTextRight() { return Math.max(nameX + 1, panel.right() - SCROLLBAR_GUTTER); }
        public int infoTextWidth() { return Math.max(1, infoTextRight() - nameX); }
        public int integerControlWidth() { return INTEGER_CONTROL_WIDTH; }
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
