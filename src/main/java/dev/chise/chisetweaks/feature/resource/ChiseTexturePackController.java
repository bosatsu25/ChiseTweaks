package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Chise Textureの選択状態をMinecraft標準のresource-pack repositoryへ反映する。 */
public final class ChiseTexturePackController {
    private ChiseTexturePackController() {}

    public static boolean isEnabled() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        return client.getResourcePackRepository()
                .getSelectedIds()
                .contains(ChiseTexturePackRegistrar.repositoryPackId());
    }

    public static void setEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        PackRepository repository = client.getResourcePackRepository();
        String packId = ChiseTexturePackRegistrar.repositoryPackId();
        if (!repository.getAvailableIds().contains(packId)) {
            ChiseTweaksClient.LOGGER.warn(
                    "Chise Texture visibility was not changed because built-in pack {} is unavailable",
                    packId);
            return;
        }

        List<String> previous = new ArrayList<>(repository.getSelectedIds());
        List<String> selected = new ArrayList<>(previous);
        boolean changed;
        if (enabled) {
            changed = !selected.contains(packId);
            if (changed) selected.add(packId);
        } else {
            changed = selected.remove(packId);
        }
        if (!changed) return;

        try {
            repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
            client.reloadResourcePacks();
        } catch (RuntimeException | LinkageError failure) {
            repository.setSelected(previous);
            ChiseTweaksClient.LOGGER.warn(
                    "Chise Texture visibility change failed; previous resource-pack selection was restored",
                    failure);
        }
    }
}
