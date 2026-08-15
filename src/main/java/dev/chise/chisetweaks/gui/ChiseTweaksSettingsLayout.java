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
    private static final int SEARCH_Y = 40;
    private static final int PANEL_Y = 68;
    private static final int HIGHLIGHT_BULK_WIDTH = 90;
    private static final int TEXT_CONTROL_GAP = 12;
    private static final int TEXT_COLUMN_GAP = 14;
    private static final int STACKED_TEXT_BREAKPOINT = 720;
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
        Rect search = new Rect(contentX, SEARCH_Y, contentWidth, 20);

        int footerY = Math.max(0, safeHeight - 26);
        int panelHeight = Math.max(1, footerY - PANEL_Y - 6);
        Rect panel = new Rect(contentX, PANEL_Y, contentWidth, panelHeight);
        Rect footer = new Rect(contentX, footerY, contentWidth, Math.min(20, safeHeight - footerY));
        FooterButtons footerButtons = footerButtons(footer);

        int controlWidth = clamp(contentWidth / 8, 72, 116);
        int controlSlotWidth = Math.max(controlWidth, INTEGER_CONTROL_WIDTH);
        int panelPadding = clamp(contentWidth / 24, 6, 16);
        int nameX = panel.x() + panelPadding;
        int controlX = Math.max(nameX, panel.right() - controlSlotWidth - panelPadding);
        int textRight = Math.max(nameX + 1, controlX - TEXT_CONTROL_GAP);
        int availableTextWidth = Math.max(1, textRight - nameX);
        boolean stackedText = contentWidth < STACKED_TEXT_BREAKPOINT;

        int nameWidth;
        int descriptionX;
        int descriptionWidth;
        int rowHeight;
        if (stackedText) {
            nameWidth = availableTextWidth;
            descriptionX = nameX;
            descriptionWidth = availableTextWidth;
            rowHeight = 42;
        } else {
            nameWidth = clamp(availableTextWidth * 32 / 100, 150, 230);
            descriptionX = nameX + nameWidth + TEXT_COLUMN_GAP;
            descriptionWidth = Math.max(1, textRight - descriptionX);
            rowHeight = 34;
        }

        int panelInset = Math.min(8, Math.max(0, panel.height() / 4));
        int bulkWidth = Math.min(HIGHLIGHT_BULK_WIDTH, Math.max(1, contentWidth - panelPadding * 2));
        Rect bulk = new Rect(
                Math.max(panel.x(), panel.right() - panelPadding - bulkWidth),
                panel.y() + panelInset + 2,
                bulkWidth,
                18);

        return new Geometry(
                content,
                search,
                bulk,
                panel,
                footer,
                footerButtons.help(),
                footerButtons.reset(),
                footerButtons.apply(),
                footerButtons.done(),
                controlWidth,
                controlSlotWidth,
                nameX,
                nameWidth,
                descriptionX,
                descriptionWidth,
                controlX,
                rowHeight,
                22,
                stackedText);
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
            Rect search,
            Rect bulk,
            Rect panel,
            Rect footer,
            Rect helpButton,
            Rect resetButton,
            Rect applyButton,
            Rect doneButton,
            int controlWidth,
            int controlSlotWidth,
            int nameX,
            int nameWidth,
            int descriptionX,
            int descriptionWidth,
            int controlX,
            int rowHeight,
            int headerHeight,
            boolean stackedText) {

        public int panelContentTop() { return panel.y() + Math.min(8, Math.max(0, panel.height() / 4)); }
        public int panelContentBottom() { return panel.bottom() - Math.min(8, Math.max(0, panel.height() / 4)); }
        public int textRight() { return controlX - TEXT_CONTROL_GAP; }
        public int booleanControlX() { return controlX + controlSlotWidth - controlWidth; }
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
