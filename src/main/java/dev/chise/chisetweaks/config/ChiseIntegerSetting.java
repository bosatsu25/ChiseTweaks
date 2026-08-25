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
    private final SettingPersistence persistence;
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
                name, defaultValue, minValue, maxValue,
                englishName, japaneseName, englishComment, japaneseComment,
                null, null, value -> Integer.toString(value), false,
                SettingPersistence.FEATURE_CONFIG);
    }

    ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            IntSupplier reader,
            IntConsumer writer,
            SettingPersistence persistence) {
        this(
                name, defaultValue, minValue, maxValue,
                name, name, "", "",
                reader, writer, value -> Integer.toString(value), true, persistence);
    }

    ChiseIntegerSetting(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            IntSupplier reader,
            IntConsumer writer,
            IntFunction<String> valueFormatter,
            SettingPersistence persistence) {
        this(
                name, defaultValue, minValue, maxValue,
                name, name, "", "",
                reader, writer, valueFormatter, true, persistence);
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
            SettingPersistence persistence) {
        this(
                name, defaultValue, minValue, maxValue,
                englishName, japaneseName, englishComment, japaneseComment,
                reader, writer, value -> Integer.toString(value), true, persistence);
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
            IntFunction<String> valueFormatter,
            SettingPersistence persistence) {
        this(
                name, defaultValue, minValue, maxValue,
                englishName, japaneseName, englishComment, japaneseComment,
                reader, writer, valueFormatter, true, persistence);
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
            boolean bound,
            SettingPersistence persistence) {
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
        this.persistence = Objects.requireNonNull(persistence, "persistence");
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
    public SettingPersistence persistence() { return persistence; }
    public String getFormattedValue() {
        int current = getIntegerValue();
        return Objects.requireNonNullElse(valueFormatter.apply(current), Integer.toString(current));
    }

    /** 実効値が変化した場合だけtrueを返す。変更後callbackはfail-softで実行する。 */
    public boolean setIntegerValue(int requested) {
        int next = clamp(requested, minValue, maxValue);
        int previous = getIntegerValue();
        if (previous == next) return false;
        writer.accept(next);
        if (getIntegerValue() == previous) return false;
        SettingChangeDispatcher.notifySafely(name, this, callback);
        return true;
    }

    public void setIntegerValueSilently(int requested) {
        int next = clamp(requested, minValue, maxValue);
        if (getIntegerValue() != next) writer.accept(next);
    }

    public boolean resetToDefault() {
        return setIntegerValue(defaultValue);
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
