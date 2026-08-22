package dev.chise.chisetweaks.feature;

/**
 * ChiseTweaks機能の基本契約。
 *
 * <p>クライアントTick処理が必要な機能だけが{@link TickingFeature}を実装する。
 * イベント駆動機能をTickディスパッチから分離し、無効時と待機時の定常コストを抑える。</p>
 */
public interface Feature {
    String getId();

    String getName();

    default void init() {
    }

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
