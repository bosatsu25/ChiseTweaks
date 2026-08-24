package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** ローカル描画機能のUIスイッチ。作業コピーだけを更新し、永続化はApply境界で行う。 */
public final class LocalFeatureSwitch extends ChiseBooleanSetting {
    private static final boolean DEFAULT_ENABLED = false;

    private final FeatureDefinition definition;
    private final Predicate<LocalFeatureConfig> getter;
    private final BiConsumer<LocalFeatureConfig, Boolean> setter;

    LocalFeatureSwitch(
            FeatureDefinition definition,
            String configName,
            Predicate<LocalFeatureConfig> getter,
            BiConsumer<LocalFeatureConfig, Boolean> setter) {
        super(
                configName,
                DEFAULT_ENABLED,
                definition.englishName(),
                definition.englishName(),
                "ChiseTweaks local visual feature.",
                "ChiseTweaks のローカル描画機能です。",
                SettingPersistence.LOCAL_CONFIG);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    public FeatureDefinition definition() {
        return definition;
    }

    @Override
    protected boolean readValue() {
        return FeatureAvailabilityPolicy.isAvailable(definition)
                && getter.test(LocalFeatureConfig.getInstance());
    }

    @Override
    protected void writeValue(boolean value) {
        boolean effectiveValue = FeatureAvailabilityPolicy.isAvailable(definition) && value;
        setter.accept(LocalFeatureConfig.getInstance(), effectiveValue);
    }
}
