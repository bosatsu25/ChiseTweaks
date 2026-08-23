package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;

/** クライアントTickで上限付き処理を行う内部ランタイムサービス。 */
public interface TickingRuntimeComponent extends RuntimeComponent {
    void tick(Minecraft client);
}
