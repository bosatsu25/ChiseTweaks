package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.gui.ChiseTweaksConfigScreen;
import dev.chise.chisetweaks.runtime.ClientInputHandler;
import dev.chise.chisetweaks.runtime.FeatureControlBindings;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Wires Chise client settings and input surfaces into the game client. */
@Environment(EnvType.CLIENT)
public final class ClientFeatureBootstrap implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(
                ChiseTweaksMetadata.MOD_ID, new FeatureConfig());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                ChiseTweaksMetadata.MOD_ID,
                ChiseTweaksMetadata.MOD_NAME,
                ChiseTweaksConfigScreen::new));

        ClientInputHandler input = ClientInputHandler.getInstance();
        InputEventHandler.getKeybindManager().registerKeybindProvider(input);
        InputEventHandler.getInputManager().registerKeyboardInputHandler(input);
        InputEventHandler.getInputManager().registerMouseInputHandler(input);
        LocalFeatureSettings.init();
        FeatureControlBindings.init();
    }
}
