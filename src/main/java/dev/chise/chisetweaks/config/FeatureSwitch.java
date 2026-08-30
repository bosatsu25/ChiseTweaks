package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** 全toggle可能Featureを同じ設定契約へ載せる。保存先や実装方式の違いはbinding内部へ閉じ込める。 */
public final class FeatureSwitch extends ChiseBooleanSetting {
    private final FeatureDefinition definition;

    FeatureSwitch(FeatureDefinition definition) {
        this(definition, configName(definition), false, null, null, SettingPersistence.FEATURE_CONFIG);
    }

    FeatureSwitch(
            FeatureDefinition definition,
            String configName,
            boolean defaultEnabled,
            BooleanSupplier reader,
            Consumer<Boolean> writer,
            SettingPersistence persistence) {
        super(
                configName,
                defaultEnabled,
                reader,
                writer,
                () -> FeatureAvailabilityPolicy.isAvailable(definition),
                persistence);
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public FeatureDefinition definition() {
        return definition;
    }

    void resetForConfigLoad() {
        resetSilently();
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
