package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.MasaJapaneseTextPolicy;
import net.minecraft.client.Minecraft;

/** Runtime adapter between the selected Minecraft language and the pure Japanese-text policy. */
public final class MasaJapaneseUiRuntime {
    private MasaJapaneseUiRuntime() {}

    public static String translate(String key, String original) {
        Minecraft client = Minecraft.getInstance();
        String languageCode = client == null || client.options == null
                ? ""
                : client.options.languageCode;
        return MasaJapaneseTextPolicy.translate(
                key,
                original,
                MasaIntegrationConfig.getInstance().japaneseUiMode,
                languageCode);
    }
}
