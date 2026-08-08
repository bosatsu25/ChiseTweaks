package dev.chise.chisetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.chise.chisetweaks.ChiseTweaksClient;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.Objects;

/**
 * Shared MaLiLib boolean-option plumbing.
 *
 * <p>Subclasses own where a value lives; this class owns translation, dirty-state,
 * callback and JSON behavior. That keeps config integration mechanics separate from
 * feature identity and runtime behavior.</p>
 */
public abstract class AbstractBooleanOption implements IConfigBoolean, IConfigNotifiable<IConfigBoolean> {
    private final String name;
    private final boolean defaultValue;
    private final String nameTranslationKey;
    private final String commentTranslationKey;
    private final String fallbackDisplayName;
    private final String fallbackComment;

    private String prettyNameKey;
    private String translatedNameKey;
    private String commentOverride;
    private boolean dirty;
    private IValueChangeCallback<IConfigBoolean> callback;

    AbstractBooleanOption(
            String name,
            boolean defaultValue,
            String nameTranslationKey,
            String commentTranslationKey,
            String fallbackDisplayName,
            String fallbackComment) {
        this.name = requireText(name, "name");
        this.defaultValue = defaultValue;
        this.nameTranslationKey = requireText(nameTranslationKey, "nameTranslationKey");
        this.commentTranslationKey = requireText(commentTranslationKey, "commentTranslationKey");
        this.fallbackDisplayName = requireText(fallbackDisplayName, "fallbackDisplayName");
        this.fallbackComment = Objects.requireNonNullElse(fallbackComment, "");
        this.prettyNameKey = this.nameTranslationKey;
        this.translatedNameKey = this.nameTranslationKey;
    }

    protected abstract boolean readValue();
    protected abstract void writeValue(boolean value);

    protected final void resetSilently() {
        writeValue(defaultValue);
        dirty = false;
    }

    @Override public final String getName() { return name; }
    @Override public final String getConfigGuiDisplayName() {
        return StringUtils.getTranslatedOrFallback(nameTranslationKey, fallbackDisplayName);
    }
    @Override public final String getPrettyName() {
        return StringUtils.getTranslatedOrFallback(prettyNameKey, fallbackDisplayName);
    }
    @Override public final String getComment() {
        String fallback = commentOverride == null ? fallbackComment : commentOverride;
        return StringUtils.getTranslatedOrFallback(commentTranslationKey, fallback);
    }
    @Override public final String getTranslatedName() {
        return StringUtils.getTranslatedOrFallback(translatedNameKey, fallbackDisplayName);
    }
    @Override public final void setPrettyName(String value) { prettyNameKey = Objects.requireNonNullElse(value, ""); }
    @Override public final void setTranslatedName(String value) { translatedNameKey = Objects.requireNonNullElse(value, ""); }
    @Override public final void setComment(String value) { commentOverride = Objects.requireNonNullElse(value, ""); }
    @Override public final String getStringValue() { return Boolean.toString(readValue()); }
    @Override public final String getDefaultStringValue() { return Boolean.toString(defaultValue); }
    @Override public final void setValueFromString(String value) { setBooleanValue(Boolean.parseBoolean(value)); }
    @Override public final void onValueChanged() { if (callback != null) callback.onValueChanged(this); }
    @Override public final void setValueChangeCallback(IValueChangeCallback<IConfigBoolean> value) { callback = value; }
    @Override public final boolean getBooleanValue() { return readValue(); }
    @Override public final boolean getDefaultBooleanValue() { return defaultValue; }

    @Override
    public final void setBooleanValue(boolean value) {
        if (readValue() == value) return;
        writeValue(value);
        onValueChanged();
    }

    @Override public final boolean isModified() { return readValue() != defaultValue; }
    @Override public final boolean isModified(String value) { return Boolean.parseBoolean(value) != defaultValue; }
    @Override public final void resetToDefault() { setBooleanValue(defaultValue); }
    @Override public final boolean isDirty() { return dirty; }
    @Override public final void markDirty() { dirty = true; }
    @Override public final void markClean() { dirty = false; }
    @Override public final void checkIfClean() { if (dirty) { markClean(); onValueChanged(); } }
    @Override public final JsonElement getAsJsonElement() { return new JsonPrimitive(readValue()); }

    @Override
    public final void setValueFromJsonElement(JsonElement element) {
        try {
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                ChiseTweaksClient.LOGGER.warn("Ignored a non-boolean value for config '{}'", name);
                return;
            }
            setBooleanValue(element.getAsBoolean());
            markClean();
        } catch (RuntimeException failure) {
            ChiseTweaksClient.LOGGER.warn("Ignored an invalid value for config '{}'", name);
        }
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
