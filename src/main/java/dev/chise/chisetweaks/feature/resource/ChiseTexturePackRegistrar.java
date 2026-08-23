package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Chise-owned high-visibility textures are registered as independently controllable built-in packs. */
public final class ChiseTexturePackRegistrar {
    private static final Identifier CHEST_PACK_ID = Identifier.fromNamespaceAndPath(
            ChiseTweaksClient.MOD_ID, "chise_chest_visibility");
    private static final Identifier WHITE_CONCRETE_PACK_ID = Identifier.fromNamespaceAndPath(
            ChiseTweaksClient.MOD_ID, "chise_white_concrete_visibility");

    private ChiseTexturePackRegistrar() {}

    public static void register() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer(ChiseTweaksClient.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("ChiseTweaks mod container is unavailable"));

        registerPack(
                CHEST_PACK_ID,
                container,
                "ChiseTweaks: Chest Visibility",
                "Chest Visibility built-in resource pack could not be registered");
        registerPack(
                WHITE_CONCRETE_PACK_ID,
                container,
                "ChiseTweaks: White Concrete Visibility",
                "White Concrete Visibility built-in resource pack could not be registered");
    }

    private static void registerPack(
            Identifier id,
            ModContainer container,
            String displayName,
            String failureMessage) {
        boolean registered = ResourceLoader.registerBuiltinPack(
                id,
                container,
                Component.literal(displayName),
                PackActivationType.DEFAULT_ENABLED);
        if (!registered) throw new IllegalStateException(failureMessage);
    }

    static String chestRepositoryPackId() {
        return CHEST_PACK_ID.toString();
    }

    static String whiteConcreteRepositoryPackId() {
        return WHITE_CONCRETE_PACK_ID.toString();
    }
}
