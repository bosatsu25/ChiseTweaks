package dev.chise.chisetweaks.mixin.gui;

import dev.chise.chisetweaks.gui.ChiseTweaksConfigScreen;
import dev.chise.chisetweaks.gui.ChiseTweaksLauncherLayout;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** Adds the standalone ChiseTweaks entry point to Minecraft's own Options screen. */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chisetweaks$addSettingsLauncher(CallbackInfo ci) {
        var occupied = new ArrayList<ChiseTweaksLauncherLayout.Bounds>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                occupied.add(new ChiseTweaksLauncherLayout.Bounds(
                        widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
            }
        }

        var placement = ChiseTweaksLauncherLayout.place(width, height, occupied);
        addRenderableWidget(Button.builder(
                Component.literal("ChiseTweaks"),
                ignored -> chisetweaks$openSettings())
                .bounds(placement.x(), placement.y(), placement.width(), placement.height())
                .build());
    }

    private void chisetweaks$openSettings() {
        if (minecraft == null) return;
        ChiseTweaksConfigScreen settings = new ChiseTweaksConfigScreen();
        settings.setParent(this);
        minecraft.setScreen(settings);
    }
}
