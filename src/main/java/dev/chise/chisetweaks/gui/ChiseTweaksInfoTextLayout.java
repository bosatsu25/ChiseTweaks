package dev.chise.chisetweaks.gui;

import java.util.List;
import java.util.Objects;

/** INFO行の折り返し結果と、そこから決まる可変高を保持する。 */
final class ChiseTweaksInfoTextLayout {
    private static final int TOP_PADDING = 6;
    private static final int SECTION_GAP = 5;
    private static final int BOTTOM_PADDING = 7;

    private ChiseTweaksInfoTextLayout() {}

    static <T> Layout<T> create(
            String name,
            String description,
            int availableWidth,
            int lineHeight,
            int minimumRowHeight,
            TextWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "wrapper");
        int safeWidth = Math.max(1, availableWidth);
        int safeLineHeight = Math.max(1, lineHeight);
        List<T> nameLines = wrapRequired(name, safeWidth, wrapper, "name");
        List<T> descriptionLines = wrapOptional(description, safeWidth, wrapper);
        int descriptionGap = descriptionLines.isEmpty() ? 0 : SECTION_GAP;
        long requiredHeight = (long) TOP_PADDING
                + (long) nameLines.size() * safeLineHeight
                + descriptionGap
                + (long) descriptionLines.size() * safeLineHeight
                + BOTTOM_PADDING;
        int rowHeight = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(Math.max(1, minimumRowHeight), requiredHeight));
        return new Layout<>(nameLines, descriptionLines, safeLineHeight, rowHeight);
    }

    private static <T> List<T> wrapRequired(
            String text,
            int availableWidth,
            TextWrapper<T> wrapper,
            String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        List<T> lines = copyLines(wrapper.wrap(text, availableWidth));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException(field + " did not produce a line");
        }
        return lines;
    }

    private static <T> List<T> wrapOptional(
            String text,
            int availableWidth,
            TextWrapper<T> wrapper) {
        if (text == null || text.isBlank()) return List.of();
        List<T> lines = copyLines(wrapper.wrap(text, availableWidth));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("description did not produce a line");
        }
        return lines;
    }

    private static <T> List<T> copyLines(List<T> lines) {
        return List.copyOf(Objects.requireNonNull(lines, "wrapped lines"));
    }

    @FunctionalInterface
    interface TextWrapper<T> {
        List<T> wrap(String text, int availableWidth);
    }

    record Layout<T>(
            List<T> nameLines,
            List<T> descriptionLines,
            int lineHeight,
            int rowHeight) {
        Layout {
            nameLines = List.copyOf(nameLines);
            descriptionLines = List.copyOf(descriptionLines);
            if (nameLines.isEmpty()) throw new IllegalArgumentException("nameLines must not be empty");
            if (lineHeight < 1) throw new IllegalArgumentException("lineHeight must be positive");
            if (rowHeight < 1) throw new IllegalArgumentException("rowHeight must be positive");
        }

        int nameLineY(int index) {
            checkIndex(index, nameLines.size());
            return TOP_PADDING + index * lineHeight;
        }

        int descriptionLineY(int index) {
            checkIndex(index, descriptionLines.size());
            return TOP_PADDING + nameLines.size() * lineHeight + SECTION_GAP + index * lineHeight;
        }

        int textBottom() {
            if (!descriptionLines.isEmpty()) {
                return descriptionLineY(descriptionLines.size() - 1) + lineHeight;
            }
            return nameLineY(nameLines.size() - 1) + lineHeight;
        }

        private static void checkIndex(int index, int size) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        }
    }
}
