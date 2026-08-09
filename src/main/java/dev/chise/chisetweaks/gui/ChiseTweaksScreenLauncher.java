package dev.chise.chisetweaks.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/** Adds a Chise-owned settings entry point without depending on Mod Menu or MaLiLib. */
@Environment(EnvType.CLIENT)
public final class ChiseTweaksScreenLauncher {
    private static boolean registered;

    private ChiseTweaksScreenLauncher() {}

    public static void register() {
        if (registered) return;
        registered = true;

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof OptionsScreen)) return;

            var buttons = Screens.getButtons(screen);
            var occupied = new ArrayList<ChiseTweaksLauncherLayout.Bounds>(buttons.size());
            for (var button : buttons) {
                occupied.add(new ChiseTweaksLauncherLayout.Bounds(
                        button.getX(), button.getY(), button.getWidth(), button.getHeight()));
            }

            var placement = ChiseTweaksLauncherLayout.place(scaledWidth, scaledHeight, occupied);
            Button launcher = Button.builder(
                    Component.literal("ChiseTweaks"),
                    ignored -> openSettings(client, screen))
                    .bounds(placement.x(), placement.y(), placement.width(), placement.height())
                    .build();
            buttons.add(launcher);
        });
    }

    private static void openSettings(net.minecraft.client.Minecraft client, Screen parent) {
        ChiseTweaksConfigScreen settings = new ChiseTweaksConfigScreen();
        settings.setParent(parent);
        client.setScreen(settings);
    }
}
