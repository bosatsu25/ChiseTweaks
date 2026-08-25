package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SimpleBooleanSetting extends ChiseBooleanSetting {
    private final BooleanSupplier reader;
    private final Consumer<Boolean> writer;
    private boolean value;

    public SimpleBooleanSetting(String name, boolean defaultValue) {
        this(
                name,
                defaultValue,
                name,
                name,
                "",
                "",
                null,
                null,
                false,
                SettingPersistence.FEATURE_CONFIG);
    }

    public SimpleBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        this(
                name,
                defaultValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                null,
                null,
                false,
                SettingPersistence.FEATURE_CONFIG);
    }

    SimpleBooleanSetting(
            String name,
            boolean defaultValue,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            SettingPersistence persistence) {
        this(
                name,
                defaultValue,
                name,
                name,
                "",
                "",
                reader,
                writer,
                true,
                persistence);
    }

    SimpleBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            SettingPersistence persistence) {
        this(
                name,
                defaultValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                reader,
                writer,
                true,
                persistence);
    }

    private SimpleBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            boolean bound,
            SettingPersistence persistence) {
        super(name, defaultValue, englishName, japaneseName, englishComment, japaneseComment, persistence);
        this.value = defaultValue;
        if (bound) {
            this.reader = Objects.requireNonNull(reader, "reader");
            this.writer = Objects.requireNonNull(writer, "writer");
        } else {
            this.reader = () -> value;
            this.writer = next -> value = next;
        }
    }

    @Override
    protected boolean readValue() {
        return reader.getAsBoolean();
    }

    @Override
    protected void writeValue(boolean value) {
        writer.accept(value);
    }
}
