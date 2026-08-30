package dev.chise.chisetweaks.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ChiseStringListSetting {
    private final String name;
    private final List<String> defaultValue;
    private final SettingPersistence persistence;
    private Consumer<ChiseStringListSetting> callback = ignored -> {};
    private List<String> value;

    public ChiseStringListSetting(
            String name,
            List<String> defaultValue,
            SettingPersistence persistence) {
        this.name = requireText(name, "name");
        this.defaultValue = List.copyOf(Objects.requireNonNull(defaultValue, "defaultValue"));
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.value = this.defaultValue;
    }

    public String getName() { return name; }
    public List<String> getStrings() { return value; }
    public SettingPersistence persistence() { return persistence; }

    public boolean setStrings(List<String> requested) {
        List<String> next = List.copyOf(Objects.requireNonNullElse(requested, List.of()));
        if (value.equals(next)) return false;
        value = next;
        SettingChangeDispatcher.markChanged();
        SettingChangeDispatcher.notifySafely(name, this, callback);
        return true;
    }

    public void setStringsSilently(List<String> requested) {
        value = List.copyOf(Objects.requireNonNullElse(requested, List.of()));
    }

    public boolean resetToDefault() { return setStrings(defaultValue); }

    public void setValueChangeCallback(Consumer<ChiseStringListSetting> value) {
        callback = value == null ? ignored -> {} : value;
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
