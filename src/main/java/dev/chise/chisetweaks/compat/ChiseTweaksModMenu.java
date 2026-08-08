package dev.chise.chisetweaks.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.chise.chisetweaks.gui.ChiseTweaksConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Mod Menu entry for the task-oriented Chise settings screen. */
@Environment(EnvType.CLIENT)
public final class ChiseTweaksModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ChiseTweaksConfigScreen settings = new ChiseTweaksConfigScreen();
            settings.setParent(parent);
            return settings;
        };
    }
}
