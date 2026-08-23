package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class ChiseBooleanSetting {
    private final String name;
    private final boolean defaultValue;
    private final String englishName;
    private final String japaneseName;
    private final String englishComment;
    private final String japaneseComment;
    private final SettingPersistence persistence;
    private final CopyOnWriteArrayList<Consumer<ChiseBooleanSetting>> additionalListeners =
            new CopyOnWriteArrayList<>();
    private Consumer<ChiseBooleanSetting> callback = ignored -> {};

    protected ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            SettingPersistence persistence) {
        this.name = requireText(name, "name");
        this.defaultValue = defaultValue;
        this.englishName = requireText(englishName, "englishName");
        this.japaneseName = requireText(japaneseName, "japaneseName");
        this.englishComment = Objects.requireNonNullElse(englishComment, "");
        this.japaneseComment = Objects.requireNonNullElse(japaneseComment, this.englishComment);
        this.persistence = Objects.requireNonNull(persistence, "persistence");
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

    public final SettingPersistence persistence() {
        return persistence;
    }

    /** 実効値が変化した場合だけtrueを返す。変更後callbackの失敗は値変更を取り消さない。 */
    public final boolean setBooleanValue(boolean value) {
        boolean previous = readValue();
        if (previous == value) return false;
        writeValue(value);
        if (readValue() == previous) return false;
        notifyChangeListeners();
        return true;
    }

    public final void setBooleanValueSilently(boolean value) {
        if (readValue() != value) writeValue(value);
    }

    public final boolean toggleBooleanValue() {
        return setBooleanValue(!readValue());
    }

    public final boolean resetToDefault() {
        return setBooleanValue(defaultValue);
    }

    public final void resetSilently() {
        setBooleanValueSilently(defaultValue);
    }

    public final void setValueChangeCallback(Consumer<ChiseBooleanSetting> value) {
        callback = value == null ? ignored -> {} : value;
    }

    public final void addValueChangeListener(Consumer<ChiseBooleanSetting> value) {
        if (value != null) additionalListeners.addIfAbsent(value);
    }

    private void notifyChangeListeners() {
        SettingChangeDispatcher.notifySafely(name, this, callback);
        for (Consumer<ChiseBooleanSetting> listener : additionalListeners) {
            SettingChangeDispatcher.notifySafely(name, this, listener);
        }
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
