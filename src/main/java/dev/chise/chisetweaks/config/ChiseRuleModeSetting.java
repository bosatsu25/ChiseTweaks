package dev.chise.chisetweaks.config;

import java.util.Objects;
import java.util.function.Consumer;

public final class ChiseRuleModeSetting {
    private final String name;
    private final ChiseRuleMode defaultValue;
    private final SettingPersistence persistence;
    private Consumer<ChiseRuleModeSetting> callback = ignored -> {};
    private ChiseRuleMode value;

    public ChiseRuleModeSetting(
            String name,
            ChiseRuleMode defaultValue,
            SettingPersistence persistence) {
        this.name = requireText(name, "name");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public ChiseRuleMode getValue() { return value; }
    public SettingPersistence persistence() { return persistence; }

    public boolean setValue(ChiseRuleMode requested) {
        ChiseRuleMode next = Objects.requireNonNullElse(requested, ChiseRuleMode.NONE);
        if (value == next) return false;
        value = next;
        SettingChangeDispatcher.notifySafely(name, this, callback);
        return true;
    }

    public boolean setValueFromString(String raw) { return setValue(ChiseRuleMode.parse(raw)); }

    public void setValueSilently(ChiseRuleMode requested) {
        value = Objects.requireNonNullElse(requested, ChiseRuleMode.NONE);
    }

    public boolean resetToDefault() { return setValue(defaultValue); }

    public void setValueChangeCallback(Consumer<ChiseRuleModeSetting> value) {
        callback = value == null ? ignored -> {} : value;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
