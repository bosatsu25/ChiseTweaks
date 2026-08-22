package dev.chise.chisetweaks.runtime;

/** ユーザー設定を所有しない内部ランタイムサービス。 */
public interface RuntimeComponent {
    String getId();
    void init();
    boolean isActive();
}
