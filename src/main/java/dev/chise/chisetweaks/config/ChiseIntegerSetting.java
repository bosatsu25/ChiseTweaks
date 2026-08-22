package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.function.Consumer;

 
public final class ChiseIntegerSetting {
    private final String name;
    private final int defaultValue;
    private final int minValue;
    private final int maxValue;
    private final String englishName;
    private final String japaneseName;
    private final String englishComment;
    private final String japaneseComment;
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
        if (minValue > maxValue) throw new IllegalArgumentException("minValue > maxValue");
        this.name = requireText(name, "name");
        this.defaultValue = clamp(defaultValue, minValue, maxValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.englishName = requireText(englishName, "englishName");
        this.japaneseName = requireText(japaneseName, "japaneseName");
        this.englishComment = Objects.requireNonNullElse(englishComment, "");
        this.japaneseComment = Objects.requireNonNullElse(japaneseComment, this.englishComment);
        this.value = this.defaultValue;
    }

    public String getName() { return name; }
    public int getIntegerValue() { return value; }
    public int getDefaultIntegerValue() { return defaultValue; }
    public int getMinIntegerValue() { return minValue; }
    public int getMaxIntegerValue() { return maxValue; }
    public String getDisplayName(boolean japanese) { return japanese ? japaneseName : englishName; }
    public String getComment(boolean japanese) { return japanese ? japaneseComment : englishComment; }

    public void setIntegerValue(int requested) {
        int next = clamp(requested, minValue, maxValue);
        if (value == next) return;
        value = next;
        callback.accept(this);
    }

    public void setIntegerValueSilently(int requested) {
        value = clamp(requested, minValue, maxValue);
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
