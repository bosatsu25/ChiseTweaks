package dev.chise.chisetweaks.runtime;

/** FeatureManagerが初期化・障害分離するクライアントランタイム要素。 */
public interface RuntimeComponent {
    String getId();
    void init();
}
