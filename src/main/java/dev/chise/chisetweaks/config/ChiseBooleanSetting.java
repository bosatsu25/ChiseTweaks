package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ChiseBooleanSetting {
    private static final BooleanSupplier AVAILABLE = () -> true;

    private final String name;
    private final boolean defaultValue;
    private final String englishName;
    private final String japaneseName;
    private final String englishComment;
    private final String japaneseComment;
    private final SettingPersistence persistence;
    private final BooleanSupplier reader;
    private final Consumer<Boolean> writer;
    private final BooleanSupplier availability;
    private final CopyOnWriteArrayList<Consumer<ChiseBooleanSetting>> additionalListeners =
            new CopyOnWriteArrayList<>();
    private Consumer<ChiseBooleanSetting> callback = ignored -> {};
    private boolean value;

    public ChiseBooleanSetting(String name, boolean defaultValue) {
        this(name, defaultValue, name, name, "", "", null, null, AVAILABLE,
                SettingPersistence.FEATURE_CONFIG);
    }

    public ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        this(name, defaultValue, englishName, japaneseName, englishComment, japaneseComment,
                null, null, AVAILABLE, SettingPersistence.FEATURE_CONFIG);
    }

    protected ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            SettingPersistence persistence) {
        this(name, defaultValue, englishName, japaneseName, englishComment, japaneseComment,
                null, null, AVAILABLE, persistence);
    }

    ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            SettingPersistence persistence) {
        this(name, defaultValue, name, name, "", "", reader, writer, AVAILABLE, persistence);
    }

    ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            SettingPersistence persistence) {
        this(name, defaultValue, englishName, japaneseName, englishComment, japaneseComment,
                reader, writer, AVAILABLE, persistence);
    }

    protected ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            BooleanSupplier availability,
            SettingPersistence persistence) {
        this(name, defaultValue, name, name, "", "", reader, writer, availability, persistence);
    }

    private ChiseBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            BooleanSupplier availability,
            SettingPersistence persistence) {
        this.name = requireText(name, "name");
        this.defaultValue = defaultValue;
        this.englishName = requireText(englishName, "englishName");
        this.japaneseName = requireText(japaneseName, "japaneseName");
        this.englishComment = Objects.requireNonNullElse(englishComment, "");
        this.japaneseComment = Objects.requireNonNullElse(japaneseComment, this.englishComment);
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.availability = Objects.requireNonNull(availability, "availability");
        this.value = defaultValue;
        if (reader == null && writer == null) {
            this.reader = () -> value;
            this.writer = next -> value = next;
        } else {
            this.reader = Objects.requireNonNull(reader, "reader");
            this.writer = Objects.requireNonNull(writer, "writer");
        }
    }

    public final String getName() { return name; }

    protected boolean readValue() {
        return availability.getAsBoolean() && reader.getAsBoolean();
    }

    protected void writeValue(boolean requested) {
        writeValue(requested);
    }

    public final boolean getBooleanValue() {
        return readValue();
    }

    public final boolean getDefaultBooleanValue() { return defaultValue; }

    public final String getDisplayName(boolean japanese) {
        return japanese ? japaneseName : englishName;
    }

    public final String getComment(boolean japanese) {
        return japanese ? japaneseComment : englishComment;
    }

    public final SettingPersistence persistence() { return persistence; }

    /** 実効値が変化した場合だけtrueを返す。変更後callbackの失敗は値変更を取り消さない。 */
    public final boolean setBooleanValue(boolean requested) {
        boolean previous = getBooleanValue();
        if (previous == requested) return false;
        writer.accept(availability.getAsBoolean() && requested);
        if (getBooleanValue() == previous) return false;
        notifyChangeListeners();
        return true;
    }

    public final void setBooleanValueSilently(boolean requested) {
        if (readValue() != requested) writeValue(requested);
    }

    public final boolean toggleBooleanValue() {
        return setBooleanValue(!getBooleanValue());
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
