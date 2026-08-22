package dev.chise.chisetweaks.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.chise.chisetweaks.gui.ChiseTweaksConfigScreen;

 





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
