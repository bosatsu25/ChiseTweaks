package dev.chise.chisetweaks.gui;

public final class ChiseTweaksSettingsLayout {
    private static final int OUTER_MARGIN = 12;
    private static final int MAX_CONTENT_WIDTH = 960;
    private static final int PANEL_Y = 38;
    private static final int CONTROL_GAP = 6;
    private static final int TEXT_CONTROL_GAP = 12;
    private static final int INTEGER_CONTROL_WIDTH = 110;
    private static final int FOOTER_GAP = 6;
    private static final int HELP_WIDTH = 92;
    private static final int RESET_WIDTH = 112;
    private static final int APPLY_WIDTH = 92;
    private static final int DONE_WIDTH = 132;

    private ChiseTweaksSettingsLayout() {}

    public static Geometry calculate(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth >= 80 ? Math.min(OUTER_MARGIN, Math.max(4, safeWidth / 24)) : 0;

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(1, safeWidth - margin * 2));
        int contentX = Math.max(0, (safeWidth - contentWidth) / 2);
        Rect content = new Rect(contentX, 0, contentWidth, safeHeight);

        int footerY = Math.max(0, safeHeight - 26);
        int panelHeight = Math.max(1, footerY - PANEL_Y - 6);
        Rect panel = new Rect(contentX, PANEL_Y, contentWidth, panelHeight);
        Rect footer = new Rect(contentX, footerY, contentWidth, Math.min(20, safeHeight - footerY));
        FooterButtons footerButtons = footerButtons(footer);

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
        int headerHeight = 24;

        int headerInset = Math.min(8, Math.max(0, panel.height() / 4));
        Rect bulk = new Rect(toggleX, panel.y() + headerInset + 3, toggleWidth, 18);
        Rect headerAction = new Rect(actionX, panel.y() + headerInset + 3, actionWidth, 18);

        return new Geometry(
                content,
                bulk,
                headerAction,
                panel,
                footer,
                footerButtons.help(),
                footerButtons.reset(),
                footerButtons.apply(),
                footerButtons.done(),
                toggleWidth,
                actionWidth,
                nameX,
                nameWidth,
                actionX,
                toggleX,
                integerX,
                rowHeight,
                headerHeight);
    }

    private static FooterButtons footerButtons(Rect footer) {
        int preferred = HELP_WIDTH + RESET_WIDTH + APPLY_WIDTH + DONE_WIDTH + FOOTER_GAP * 3;
        if (footer.width() >= preferred) {
            Rect help = new Rect(footer.x(), footer.y(), HELP_WIDTH, footer.height());
            Rect reset = new Rect(help.right() + FOOTER_GAP, footer.y(), RESET_WIDTH, footer.height());
            Rect apply = new Rect(reset.right() + FOOTER_GAP, footer.y(), APPLY_WIDTH, footer.height());
            Rect done = new Rect(footer.right() - DONE_WIDTH, footer.y(), DONE_WIDTH, footer.height());
            return new FooterButtons(help, reset, apply, done);
        }

        int usable = Math.max(4, footer.width() - FOOTER_GAP * 3);
        int helpWidth = usable / 4;
        int resetWidth = usable / 4;
        int applyWidth = usable / 4;
        int doneWidth = usable - helpWidth - resetWidth - applyWidth;
        Rect help = new Rect(footer.x(), footer.y(), helpWidth, footer.height());
        Rect reset = new Rect(help.right() + FOOTER_GAP, footer.y(), resetWidth, footer.height());
        Rect apply = new Rect(reset.right() + FOOTER_GAP, footer.y(), applyWidth, footer.height());
        Rect done = new Rect(apply.right() + FOOTER_GAP, footer.y(), doneWidth, footer.height());
        return new FooterButtons(help, reset, apply, done);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record FooterButtons(Rect help, Rect reset, Rect apply, Rect done) {}

    public record Geometry(
            Rect content,
            Rect bulk,
            Rect headerAction,
            Rect panel,
            Rect footer,
            Rect helpButton,
            Rect resetButton,
            Rect applyButton,
            Rect doneButton,
            int toggleWidth,
            int actionWidth,
            int nameX,
            int nameWidth,
            int actionX,
            int toggleX,
            int integerX,
            int rowHeight,
            int headerHeight) {

        public int panelContentTop() { return panel.y() + Math.min(8, Math.max(0, panel.height() / 4)); }
        public int panelContentBottom() { return panel.bottom() - Math.min(8, Math.max(0, panel.height() / 4)); }
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
