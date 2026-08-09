package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.gui.ChiseTweaksHotkeyScreen;
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

        // MaLiLib's registry contract requires a GuiBase. The user-facing Mod Menu entry is the
        // Chise-owned Screen; this fallback keeps MaLiLib's own config registry functional by
        // opening the dedicated multi-key editor instead of forcing the main UI back onto GuiBase.
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                ChiseTweaksMetadata.MOD_ID,
                ChiseTweaksMetadata.MOD_NAME,
                ChiseTweaksHotkeyScreen::new));

        ClientInputHandler input = ClientInputHandler.getInstance();
        InputEventHandler.getKeybindManager().registerKeybindProvider(input);
        InputEventHandler.getInputManager().registerKeyboardInputHandler(input);
        InputEventHandler.getInputManager().registerMouseInputHandler(input);
        LocalFeatureSettings.init();
        FeatureControlBindings.init();
    }
}
