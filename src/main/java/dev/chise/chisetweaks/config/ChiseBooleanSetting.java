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
    private final CopyOnWriteArrayList<Consumer<ChiseBooleanSetting>> additionalListeners =
            new CopyOnWriteArrayList<>();
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

    /** ApplyでChise設定ファイルへ永続化する設定かを返す。外部永続化を持つ設定だけoverrideする。 */
    public boolean requiresApplyPersistence() {
        return true;
    }

    public final void setBooleanValue(boolean value) {
        boolean previous = readValue();
        if (previous == value) return;
        writeValue(value);
        if (readValue() == previous) return;
        notifyChangeListeners();
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

    public final void addValueChangeListener(Consumer<ChiseBooleanSetting> value) {
        if (value != null) additionalListeners.addIfAbsent(value);
    }

    private void notifyChangeListeners() {
        Throwable firstFailure = null;
        try {
            callback.accept(this);
        } catch (RuntimeException | LinkageError failure) {
            firstFailure = failure;
        }
        for (Consumer<ChiseBooleanSetting> listener : additionalListeners) {
            try {
                listener.accept(this);
            } catch (RuntimeException | LinkageError failure) {
                if (firstFailure == null) firstFailure = failure;
                else firstFailure.addSuppressed(failure);
            }
        }
        if (firstFailure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (firstFailure instanceof LinkageError linkageFailure) throw linkageFailure;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
