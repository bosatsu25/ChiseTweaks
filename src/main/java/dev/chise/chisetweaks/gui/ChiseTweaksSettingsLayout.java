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
    private static final int DEFAULT_NAV_COUNT = 3;
    private static final int BULK_GAP = 8;
    private static final int BULK_WIDTH = 134;
    private static final int MIN_INLINE_SEARCH_WIDTH = 150;
    private static final int TEXT_CONTROL_GAP = 12;
    private static final int TEXT_COLUMN_GAP = 14;
    private static final int STACKED_TEXT_BREAKPOINT = 720;
    private static final int INTEGER_CONTROL_WIDTH = 110;
    private static final int FOOTER_GAP = 6;
    private static final int RESET_WIDTH = 112;
    private static final int APPLY_WIDTH = 92;
    private static final int DONE_WIDTH = 132;

    private ChiseTweaksSettingsLayout() {}

    /**
     * Compatibility overload for callers that do not supply the navigation count.
     * Runtime UI code should use the overload that passes the current section count so a future
     * section addition/removal cannot silently desynchronize geometry from the rendered buttons.
     */
    public static Geometry calculate(int screenWidth, int screenHeight, boolean categoryPage) {
        return calculate(screenWidth, screenHeight, categoryPage, DEFAULT_NAV_COUNT);
    }

    public static Geometry calculate(
            int screenWidth,
            int screenHeight,
            boolean categoryPage,
            int navigationCount) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth >= 80 ? Math.min(OUTER_MARGIN, Math.max(4, safeWidth / 24)) : 0;

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(1, safeWidth - margin * 2));
        int contentX = Math.max(0, (safeWidth - contentWidth) / 2);

        // Chise is now a single standalone settings surface, so there is no mod-selector widget.
        Rect selector = Rect.EMPTY;

        int navCount = Math.max(1, navigationCount);
        int navGap = contentWidth >= 240 ? NAV_GAP : 2;
        int navAvailable = Math.max(navCount, contentWidth - navGap * (navCount - 1));
        int navButtonWidth = Math.min(112, Math.max(1, navAvailable / navCount));
        int navTotalWidth = navButtonWidth * navCount + navGap * (navCount - 1);
        Rect navigation = new Rect(contentX, 40, Math.min(contentWidth, navTotalWidth), 20);

        int searchY = 68;
        boolean toolbarStacked = categoryPage
                && contentWidth < MIN_INLINE_SEARCH_WIDTH + BULK_GAP + BULK_WIDTH;
        Rect search;
        Rect bulk;
        int panelY;
        if (!categoryPage) {
            search = new Rect(contentX, searchY, contentWidth, 20);
            bulk = Rect.EMPTY;
            panelY = 98;
        } else if (toolbarStacked) {
            search = new Rect(contentX, searchY, contentWidth, 20);
            bulk = new Rect(contentX, searchY + 24, contentWidth, 20);
            panelY = 122;
        } else {
            int searchWidth = contentWidth - BULK_WIDTH - BULK_GAP;
            search = new Rect(contentX, searchY, searchWidth, 20);
            bulk = new Rect(contentX + contentWidth - BULK_WIDTH, searchY, BULK_WIDTH, 20);
            panelY = 98;
        }

        int footerY = Math.max(0, safeHeight - 26);
        int panelHeight = Math.max(1, footerY - panelY - 6);
        Rect panel = new Rect(contentX, panelY, contentWidth, panelHeight);
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

        return new Geometry(
                new Rect(contentX, 0, contentWidth, safeHeight),
                selector,
                navigation,
                search,
                bulk,
                panel,
                footer,
                footerButtons.reset(),
                footerButtons.apply(),
                footerButtons.done(),
                navButtonWidth,
                navGap,
                navCount,
                controlWidth,
                controlSlotWidth,
                nameX,
                nameWidth,
                descriptionX,
                descriptionWidth,
                controlX,
                rowHeight,
                22,
                stackedText,
                toolbarStacked);
    }

    private static FooterButtons footerButtons(Rect footer) {
        int preferred = RESET_WIDTH + APPLY_WIDTH + DONE_WIDTH + FOOTER_GAP * 2;
        if (footer.width() >= preferred) {
            Rect reset = new Rect(footer.x(), footer.y(), RESET_WIDTH, footer.height());
            Rect apply = new Rect(reset.right() + FOOTER_GAP, footer.y(), APPLY_WIDTH, footer.height());
            Rect done = new Rect(footer.right() - DONE_WIDTH, footer.y(), DONE_WIDTH, footer.height());
            return new FooterButtons(reset, apply, done);
        }

        int usable = Math.max(3, footer.width() - FOOTER_GAP * 2);
        int resetWidth = usable / 3;
        int applyWidth = usable / 3;
        int doneWidth = usable - resetWidth - applyWidth;
        Rect reset = new Rect(footer.x(), footer.y(), resetWidth, footer.height());
        Rect apply = new Rect(reset.right() + FOOTER_GAP, footer.y(), applyWidth, footer.height());
        Rect done = new Rect(apply.right() + FOOTER_GAP, footer.y(), doneWidth, footer.height());
        return new FooterButtons(reset, apply, done);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record FooterButtons(Rect reset, Rect apply, Rect done) {}

    public record Geometry(
            Rect content,
            Rect selector,
            Rect navigation,
            Rect search,
            Rect bulk,
            Rect panel,
            Rect footer,
            Rect resetButton,
            Rect applyButton,
            Rect doneButton,
            int navButtonWidth,
            int navGap,
            int navigationCount,
            int controlWidth,
            int controlSlotWidth,
            int nameX,
            int nameWidth,
            int descriptionX,
            int descriptionWidth,
            int controlX,
            int rowHeight,
            int headerHeight,
            boolean stackedText,
            boolean toolbarStacked) {

        public boolean categoryPage() { return !bulk.isEmpty(); }
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
