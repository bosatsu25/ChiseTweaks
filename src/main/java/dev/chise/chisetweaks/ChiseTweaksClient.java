package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.feature.rendering.model.ChiseVisualModelPlugin;
import dev.chise.chisetweaks.feature.resource.ChiseTexturePackRegistrar;
import dev.chise.chisetweaks.runtime.ClientSessionState;
import dev.chise.chisetweaks.runtime.FeatureControlBindings;
import dev.chise.chisetweaks.runtime.FeatureManager;
import dev.chise.chisetweaks.runtime.SafeStartup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 建築支援と視認改善機能を初期化するクライアント側エントリーポイント。 */
@Environment(EnvType.CLIENT)
public final class ChiseTweaksClient implements ClientModInitializer {
    public static final String MOD_ID = "chisetweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SafeStartup.run("local-config", () -> LocalFeatureConfig.getInstance().load());
        SafeStartup.run("feature-config", FeatureConfig::loadFromFile);
        SafeStartup.run("chise-texture-pack", ChiseTexturePackRegistrar::register);
        SafeStartup.run("local-settings", LocalFeatureSettings::init);
        SafeStartup.run("visual-target-settings", VisualTargetSettings::init);
        SafeStartup.run("feature-bindings", FeatureControlBindings::init);
        SafeStartup.run("visual-model-plugin", ChiseVisualModelPlugin::register);
        SafeStartup.run("feature-manager", () -> FeatureManager.getInstance().init());
        SafeStartup.run("connection-lifecycle", () -> {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                    ClientSessionState.onJoin(client));
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                    ClientSessionState.onDisconnect(client));
        });
        LOGGER.info(
                "ChiseTweaks {} standalone client initialized with {} isolated startup failure(s)",
                ChiseTweaksMetadata.MOD_VERSION,
                SafeStartup.failures().size());
    }
}
