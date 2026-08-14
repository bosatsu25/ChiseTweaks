package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;

/** Internal runtime service that performs bounded work on the client tick. */
public interface TickingRuntimeComponent extends RuntimeComponent {
    void tick(Minecraft client);

    /** Releases session-local state when the tick dispatcher quarantines this component. */
    default void onQuarantined(Minecraft client) {}
}
