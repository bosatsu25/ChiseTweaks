package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.feature.rendering.model.ChiseVisualModelPlugin;
import dev.chise.chisetweaks.runtime.ClientSessionState;
import dev.chise.chisetweaks.runtime.FeatureManager;
import dev.chise.chisetweaks.runtime.SafeStartup;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entry point for bounded building-assistance and visual ChiseTweaks features. */
@Environment(EnvType.CLIENT)
public final class ChiseTweaksClient implements ClientModInitializer {
    public static final String MOD_ID = "chisetweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SafeStartup.run("local-config", () -> LocalFeatureConfig.getInstance().load());
        SafeStartup.run("visual-model-plugin", ChiseVisualModelPlugin::register);
        SafeStartup.run("malilib-bootstrap", () ->
                InitializationHandler.getInstance().registerInitializationHandler(new ClientFeatureBootstrap()));
        SafeStartup.run("feature-manager", () -> FeatureManager.getInstance().init());
        SafeStartup.run("connection-lifecycle", () -> {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                    ClientSessionState.onJoin(client));
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                    ClientSessionState.onDisconnect(client));
        });
        LOGGER.info(
                "ChiseTweaks {} client build initialized with {} isolated startup failure(s)",
                ChiseTweaksMetadata.MOD_VERSION,
                SafeStartup.failures().size());
    }
}
