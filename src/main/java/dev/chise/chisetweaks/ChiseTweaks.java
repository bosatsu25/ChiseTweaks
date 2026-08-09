package dev.chise.chisetweaks;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ModInitializer;

/**
 * Early client-only bootstrap used to register ChiseTweaks with MaLiLib before
 * MaLiLib dispatches its mod-handler initialization phase.
 */
public final class ChiseTweaks implements ModInitializer {
    @Override
    public void onInitialize() {
        InitializationHandler.getInstance()
                .registerInitializationHandler(new ClientFeatureBootstrap());
    }
}
