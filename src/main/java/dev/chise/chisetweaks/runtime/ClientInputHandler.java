package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;

import java.util.List;

/** Registers user-facing feature hotkeys with MaLiLib. */
public final class ClientInputHandler implements IKeybindProvider, IKeyboardInputHandler, IMouseInputHandler {
    private static final ClientInputHandler INSTANCE = new ClientInputHandler();
    private static final List<FeatureSwitch> FEATURE_KEYS = FeatureSwitches.VALUES;

    private ClientInputHandler() {}

    public static ClientInputHandler getInstance() { return INSTANCE; }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (FeatureSwitch feature : FEATURE_KEYS) manager.addKeybindToMap(feature.getKeybind());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                ChiseTweaksMetadata.MOD_NAME,
                "chisetweaks.hotkeys.category.features",
                FEATURE_KEYS);
    }
}
