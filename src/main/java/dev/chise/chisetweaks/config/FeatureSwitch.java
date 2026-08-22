package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;

import java.util.Objects;

public final class FeatureSwitch extends ChiseBooleanSetting {
    private static final boolean DEFAULT_ENABLED = false;

    private final FeatureDefinition definition;
    private boolean enabled;

    FeatureSwitch(FeatureDefinition definition, String fallbackComment) {
        super(
                configName(definition),
                DEFAULT_ENABLED,
                definition.englishName(),
                definition.englishName(),
                fallbackComment,
                fallbackComment);
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public FeatureDefinition definition() {
        return definition;
    }

    void resetForConfigLoad() {
        resetSilently();
    }

    @Override
    protected boolean readValue() {
        return PreReleaseFeaturePolicy.isAvailable(definition) && enabled;
    }

    @Override
    protected void writeValue(boolean value) {
        enabled = PreReleaseFeaturePolicy.isAvailable(definition) && value;
    }

    @Override
    public String toString() {
        return "FeatureSwitch[" + definition.id() + "]";
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
