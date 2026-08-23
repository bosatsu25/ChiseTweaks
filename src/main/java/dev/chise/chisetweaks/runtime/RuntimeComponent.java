package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;

/** FeatureManagerが初期化・障害分離するクライアントランタイム要素。 */
public interface RuntimeComponent {
    String getId();
    void init();

    /** init/tick/sessionのいずれで隔離されても呼ばれる共通cleanup境界。 */
    default void onQuarantined(Minecraft client) {}
}
