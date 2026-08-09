package dev.chise.chisetweaks.config;

/** In-memory boolean setting used for Chise-owned persisted configuration. */
public final class SimpleBooleanSetting extends ChiseBooleanSetting {
    private boolean value;

    public SimpleBooleanSetting(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        super(name, defaultValue, englishName, japaneseName, englishComment, japaneseComment);
        this.value = defaultValue;
    }

    @Override
    protected boolean readValue() {
        return value;
    }

    @Override
    protected void writeValue(boolean value) {
        this.value = value;
    }
}
