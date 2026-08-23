package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class ChiseIntegerSetting {
    private final String name;
    private final int defaultValue;
    private final int minValue;
    private final int maxValue;
    private final String englishName;
    private final String japaneseName;
    private final String englishComment;
    private final String japaneseComment;
    private final IntSupplier reader;
    private final IntConsumer writer;
    private final IntFunction<String> valueFormatter;
    private Consumer<ChiseIntegerSetting> callback = ignored -> {};
    private int value;

    public ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        this(
                name,
                defaultValue,
                minValue,
                maxValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                null,
                null,
                value -> Integer.toString(value),
                false);
    }

    ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            IntSupplier reader,
            IntConsumer writer) {
        this(
                name,
                defaultValue,
                minValue,
                maxValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                reader,
                writer,
                value -> Integer.toString(value),
                true);
    }

    ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            IntSupplier reader,
            IntConsumer writer,
            IntFunction<String> valueFormatter) {
        this(
                name,
                defaultValue,
                minValue,
                maxValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                reader,
                writer,
                valueFormatter,
                true);
    }

    private ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            IntSupplier reader,
            IntConsumer writer,
            IntFunction<String> valueFormatter,
            boolean bound) {
        if (minValue > maxValue) throw new IllegalArgumentException("minValue > maxValue");
        this.name = requireText(name, "name");
        this.defaultValue = clamp(defaultValue, minValue, maxValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.englishName = requireText(englishName, "englishName");
        this.japaneseName = requireText(japaneseName, "japaneseName");
        this.englishComment = Objects.requireNonNullElse(englishComment, "");
        this.japaneseComment = Objects.requireNonNullElse(japaneseComment, this.englishComment);
        this.valueFormatter = Objects.requireNonNull(valueFormatter, "valueFormatter");
        this.value = this.defaultValue;
        if (bound) {
            this.reader = Objects.requireNonNull(reader, "reader");
            this.writer = Objects.requireNonNull(writer, "writer");
        } else {
            this.reader = () -> value;
            this.writer = next -> value = next;
        }
    }

    public String getName() { return name; }
    public int getIntegerValue() { return clamp(reader.getAsInt(), minValue, maxValue); }
    public int getDefaultIntegerValue() { return defaultValue; }
    public int getMinIntegerValue() { return minValue; }
    public int getMaxIntegerValue() { return maxValue; }
    public String getDisplayName(boolean japanese) { return japanese ? japaneseName : englishName; }
    public String getComment(boolean japanese) { return japanese ? japaneseComment : englishComment; }
    public String getFormattedValue() {
        return Objects.requireNonNullElse(
                valueFormatter.apply(getIntegerValue()),
                Integer.toString(getIntegerValue()));
    }

    public void setIntegerValue(int requested) {
        int next = clamp(requested, minValue, maxValue);
        if (getIntegerValue() == next) return;
        writer.accept(next);
        callback.accept(this);
    }

    public void setIntegerValueSilently(int requested) {
        int next = clamp(requested, minValue, maxValue);
        if (getIntegerValue() != next) writer.accept(next);
    }

    public void resetToDefault() {
        setIntegerValue(defaultValue);
    }

    public void setValueChangeCallback(Consumer<ChiseIntegerSetting> value) {
        callback = value == null ? ignored -> {} : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
