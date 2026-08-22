package dev.chise.chisetweaks.feature;

import dev.chise.chisetweaks.runtime.RuntimeComponent;

/**
 * ChiseTweaksのユーザー向けランタイム機能契約。
 *
 * <p>機能も内部サービスも同じ{@link RuntimeComponent}ライフサイクルで管理し、
 * ユーザー向け機能だけ表示名と有効状態を追加する。</p>
 */
public interface Feature extends RuntimeComponent {
    String getName();

    @Override
    default void init() {
    }

    boolean isEnabled();
}
