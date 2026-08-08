package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;

/** Runtime state that must be cleared when the client session changes. */
public interface SessionAwareRuntimeComponent {
    void resetSession(Minecraft client);
}
