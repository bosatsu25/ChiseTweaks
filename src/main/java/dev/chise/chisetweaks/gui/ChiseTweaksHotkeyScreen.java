package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Dedicated MaLiLib-backed editor for multi-key bindings. */
public final class ChiseTweaksHotkeyScreen extends GuiConfigsBase {
    public ChiseTweaksHotkeyScreen() {
        this(null);
    }

    public ChiseTweaksHotkeyScreen(Screen parent) {
        super(10, 42, ChiseTweaksMetadata.MOD_ID, null,
                ChiseTweaksMetadata.MOD_NAME + " %s - Keybinds", ChiseTweaksMetadata.MOD_VERSION);
        if (parent != null) setParent(parent);
    }

    @Override
    protected int getConfigWidth() {
        return Math.min(260, Math.max(150, this.width / 4));
    }

    @Override
    protected boolean useKeybindSearch() {
        return true;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        ArrayList<BooleanHotkeyGuiWrapper> rows = new ArrayList<>();
        for (FeatureSwitch feature : FeatureSwitches.VALUES) {
            rows.add(wrap(feature));
        }
        return ConfigOptionWrapper.createFor(rows);
    }

    private static BooleanHotkeyGuiWrapper wrap(IHotkeyTogglable config) {
        return new BooleanHotkeyGuiWrapper(config.getName(), config, config.getKeybind());
    }
}
