package dev.chise.chisetweaks.feature;

import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;

/** 上限付きのクライアントTick処理も必要とするユーザー向け機能。 */
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
