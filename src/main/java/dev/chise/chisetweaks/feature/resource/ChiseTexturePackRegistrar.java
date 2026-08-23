package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** 高視認テクスチャを独立切替可能なbuilt-in resource packとして登録する。 */
public final class ChiseTexturePackRegistrar {
    private ChiseTexturePackRegistrar() {}

    public static void register() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer(ChiseTweaksClient.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("ChiseTweaks mod container is unavailable"));

        for (VisibilityPack pack : VisibilityPack.values()) {
            registerPack(pack, container);
        }
    }

    private static void registerPack(VisibilityPack pack, ModContainer container) {
        boolean registered = ResourceLoader.registerBuiltinPack(
                Identifier.fromNamespaceAndPath(ChiseTweaksClient.MOD_ID, pack.path()),
                container,
                Component.literal("ChiseTweaks: " + pack.displayName()),
                PackActivationType.DEFAULT_ENABLED);
        if (!registered) {
            throw new IllegalStateException(pack.displayName() + " built-in resource pack could not be registered");
        }
    }

    static String chestRepositoryPackId() {
        return VisibilityPack.CHEST.repositoryPackId();
    }

    static String whiteConcreteRepositoryPackId() {
        return VisibilityPack.WHITE_CONCRETE.repositoryPackId();
    }
}
