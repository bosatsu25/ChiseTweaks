package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;

/** クライアントセッション切替時に破棄する必要があるランタイム状態。 */
public interface SessionAwareRuntimeComponent {
    void resetSession(Minecraft client);
}
