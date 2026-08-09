package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.Locale;
import java.util.Objects;

/** Mutable MaLiLib hotkey adapter for one immutable Chise feature definition. */
public final class FeatureSwitch extends AbstractBooleanOption implements IHotkeyTogglable {
    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_HOTKEY = "";

    private final FeatureDefinition definition;
    private final IKeybind keybind;
    private final IConfigBoolean booleanGuiView;
    private boolean enabled;

    FeatureSwitch(FeatureDefinition definition, String fallbackComment) {
        super(
                configName(definition),
                DEFAULT_ENABLED,
                definition.nameKey(),
                commentKey(definition),
                definition.englishName(),
                fallbackComment);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.keybind = KeybindMulti.fromStorageString(DEFAULT_HOTKEY, KeybindSettings.DEFAULT);
        this.keybind.setCallback(new KeyCallbackToggleBooleanConfigWithMessage(this));
        this.booleanGuiView = new AbstractBooleanOption(
                configName(definition),
                DEFAULT_ENABLED,
                definition.nameKey(),
                commentKey(definition),
                definition.englishName(),
                fallbackComment) {
            @Override public ConfigType getType() { return ConfigType.BOOLEAN; }
            @Override protected boolean readValue() { return FeatureSwitch.this.getBooleanValue(); }
            @Override protected void writeValue(boolean value) { FeatureSwitch.this.setBooleanValue(value); }
        };
    }

    public FeatureDefinition definition() { return definition; }

    /**
     * Boolean-only view used by task/category pages.
     *
     * <p>The authoritative value remains this FeatureSwitch; the adapter only changes how MaLiLib
     * renders the row. This keeps large keybind controls off category pages while the dedicated
     * Keybinds page continues to expose the full multi-key editor.</p>
     */
    public IConfigBoolean booleanGuiView() { return booleanGuiView; }

    void resetForConfigLoad() { resetSilently(); }

    @Override public ConfigType getType() { return ConfigType.HOTKEY; }
    @Override public IKeybind getKeybind() { return keybind; }
    @Override protected boolean readValue() { return enabled; }
    @Override protected void writeValue(boolean value) { enabled = value; }

    @Override public String toString() { return "FeatureSwitch[" + definition.id() + "]"; }

    private static String commentKey(FeatureDefinition definition) {
        return "config.comment." + configName(definition).toLowerCase(Locale.ROOT);
    }

    private static String configName(FeatureDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String id = definition.id();
        StringBuilder result = new StringBuilder(id.length());
        boolean uppercaseNext = false;
        for (int i = 0; i < id.length(); i++) {
            char current = id.charAt(i);
            if (current == '_') {
                uppercaseNext = true;
            } else {
                result.append(uppercaseNext ? Character.toUpperCase(current) : current);
                uppercaseNext = false;
            }
        }
        return result.toString();
    }
}
