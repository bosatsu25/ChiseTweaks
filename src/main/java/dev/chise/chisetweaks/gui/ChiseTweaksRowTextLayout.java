package dev.chise.chisetweaks.gui;

import java.util.Objects;
import java.util.function.ToIntFunction;

/** Pure two-line wrapping policy for responsive settings-row descriptions. */
public final class ChiseTweaksRowTextLayout {
    private static final String ELLIPSIS = "…";

    private ChiseTweaksRowTextLayout() {}

    public static WrappedLines wrap(String rawText, int maxWidth, ToIntFunction<String> width) {
        Objects.requireNonNull(width, "width");
        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty() || maxWidth <= 0) return new WrappedLines("", "");
        if (width.applyAsInt(text) <= maxWidth) return new WrappedLines(text, "");

        int firstLimit = fittingPrefix(text, maxWidth, width);
        if (firstLimit <= 0) return new WrappedLines(ellipsize(text, maxWidth, width), "");

        int firstBreak = preferredBreak(text, firstLimit);
        String first = text.substring(0, firstBreak).stripTrailing();
        String remaining = text.substring(firstBreak).stripLeading();
        if (remaining.isEmpty()) return new WrappedLines(first, "");
        if (width.applyAsInt(remaining) <= maxWidth) return new WrappedLines(first, remaining);
        return new WrappedLines(first, ellipsize(remaining, maxWidth, width));
    }

    private static int preferredBreak(String text, int limit) {
        int minimumUsefulBreak = Math.max(1, limit / 2);
        for (int index = Math.min(limit, text.length() - 1); index >= minimumUsefulBreak; index--) {
            if (Character.isWhitespace(text.charAt(index))) return index;
        }
        return limit;
    }

    private static int fittingPrefix(String text, int maxWidth, ToIntFunction<String> width) {
        int end = 0;
        int lastFitting = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (width.applyAsInt(text.substring(0, next)) > maxWidth) break;
            lastFitting = next;
            end = next;
        }
        return lastFitting;
    }

    private static String ellipsize(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (width.applyAsInt(text) <= maxWidth) return text;
        if (width.applyAsInt(ELLIPSIS) > maxWidth) return "";

        int end = 0;
        int lastFitting = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (width.applyAsInt(text.substring(0, next) + ELLIPSIS) > maxWidth) break;
            lastFitting = next;
            end = next;
        }
        return text.substring(0, lastFitting).stripTrailing() + ELLIPSIS;
    }

    public record WrappedLines(String first, String second) {
        public int lineCount() {
            if (first.isEmpty()) return 0;
            return second.isEmpty() ? 1 : 2;
        }
    }
}
