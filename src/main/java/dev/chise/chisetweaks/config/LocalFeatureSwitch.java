package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.Feature;
import dev.chise.chisetweaks.runtime.FeatureManager;
import fi.dy.masa.malilib.config.ConfigType;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** UI adapter for a boolean stored in the bounded local Chise configuration. */
public final class LocalFeatureSwitch extends AbstractBooleanOption {
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
                definition.nameKey(),
                "config.comment." + configName.toLowerCase(java.util.Locale.ROOT),
                definition.englishName(),
                "ChiseTweaks local visual feature.");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    public FeatureDefinition definition() { return definition; }

    @Override public ConfigType getType() { return ConfigType.BOOLEAN; }
    @Override protected boolean readValue() { return getter.test(LocalFeatureConfig.getInstance()); }

    @Override
    protected void writeValue(boolean value) {
        Feature runtimeFeature = FeatureManager.getInstance().getFeature(definition.id());
        if (runtimeFeature != null) {
            runtimeFeature.setEnabled(value);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        setter.accept(config, value);
        config.save();
    }
}
