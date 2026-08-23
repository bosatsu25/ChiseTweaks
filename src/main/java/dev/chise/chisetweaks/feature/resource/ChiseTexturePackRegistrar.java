package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Chise TextureをFabric組み込みリソースパックとして登録する。 */
public final class ChiseTexturePackRegistrar {
    private static final String PACK_PATH = "chise_texture";
    private static final Identifier PACK_ID =
            Identifier.fromNamespaceAndPath(ChiseTweaksClient.MOD_ID, PACK_PATH);

    private ChiseTexturePackRegistrar() {}

    public static void register() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer(ChiseTweaksClient.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("ChiseTweaks mod container is unavailable"));

        boolean registered = ResourceLoader.registerBuiltinPack(
                PACK_ID,
                container,
                Component.literal("ChiseTweaks: Chise Texture"),
                PackActivationType.DEFAULT_ENABLED);
        if (!registered) {
            throw new IllegalStateException("Chise Texture built-in resource pack could not be registered");
        }
    }

    static String repositoryPackId() {
        return PACK_ID.toString();
    }
}
