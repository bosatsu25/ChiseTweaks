package dev.chise.chisetweaks.feature;

import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;

/** A user-facing feature that also requires bounded client-tick work. */
public interface TickingFeature extends Feature, TickingRuntimeComponent {
    @Override
    default void init() {
        Feature.super.init();
    }

    @Override
    default boolean isActive() {
        return isEnabled();
    }
}
