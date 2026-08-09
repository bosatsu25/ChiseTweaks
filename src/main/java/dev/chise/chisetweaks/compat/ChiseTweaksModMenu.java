package dev.chise.chisetweaks.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.chise.chisetweaks.gui.ChiseTweaksConfigScreen;

/**
 * Optional Mod Menu adapter.
 *
 * <p>ChiseTweaks owns its settings UI and does not require Mod Menu at runtime.
 * When Mod Menu is installed, this adapter only contributes its standard config button.
 */
public final class ChiseTweaksModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ChiseTweaksConfigScreen screen = new ChiseTweaksConfigScreen();
            screen.setParent(parent);
            return screen;
        };
    }
}
