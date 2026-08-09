package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Chise-owned boolean setting contract with no external config-library dependency.
 *
 * <p>Subclasses decide where the authoritative value lives. This class owns default handling,
 * callback delivery and bilingual UI metadata so config/runtime code can stay independent from
 * any other Minecraft mod.</p>
 */
public abstract class ChiseBooleanSetting {
    private final String name;
    private final boolean defaultValue;
    private final String englishName;
    private final String japaneseName;
    private final String englishComment;
    private final String japaneseComment;
    private Consumer<ChiseBooleanSetting> callback = ignored -> {};

    protected ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        this.name = requireText(name, "name");
        this.defaultValue = defaultValue;
        this.englishName = requireText(englishName, "englishName");
        this.japaneseName = requireText(japaneseName, "japaneseName");
        this.englishComment = Objects.requireNonNullElse(englishComment, "");
        this.japaneseComment = Objects.requireNonNullElse(japaneseComment, this.englishComment);
    }

    protected abstract boolean readValue();
    protected abstract void writeValue(boolean value);

    public final String getName() {
        return name;
    }

    public final boolean getBooleanValue() {
        return readValue();
    }

    public final boolean getDefaultBooleanValue() {
        return defaultValue;
    }

    public final String getDisplayName(boolean japanese) {
        return japanese ? japaneseName : englishName;
    }

    public final String getComment(boolean japanese) {
        return japanese ? japaneseComment : englishComment;
    }

    public final void setBooleanValue(boolean value) {
        if (readValue() == value) return;
        writeValue(value);
        callback.accept(this);
    }

    public final void setBooleanValueSilently(boolean value) {
        if (readValue() != value) writeValue(value);
    }

    public final void toggleBooleanValue() {
        setBooleanValue(!readValue());
    }

    public final void resetToDefault() {
        setBooleanValue(defaultValue);
    }

    public final void resetSilently() {
        setBooleanValueSilently(defaultValue);
    }

    public final void setValueChangeCallback(Consumer<ChiseBooleanSetting> value) {
        callback = value == null ? ignored -> {} : value;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
