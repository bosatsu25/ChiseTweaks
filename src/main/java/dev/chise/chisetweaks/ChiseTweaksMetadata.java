package dev.chise.chisetweaks;

import net.fabricmc.loader.api.FabricLoader;

/** ChiseTweaks本体の安定した製品メタデータと任意描画互換用の識別子。 */
public final class ChiseTweaksMetadata {
    public static final String MOD_ID = "chisetweaks";
    public static final String MOD_NAME = "ChiseTweaks";
    public static final String CONFIG_NAMESPACE = "chise_tweaks";
    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow(() -> new IllegalStateException("Missing ChiseTweaks mod container"))
            .getMetadata().getVersion().getFriendlyString();

    private ChiseTweaksMetadata() {}

    public static final class ExternalModIds {
        public static final String SODIUM = "sodium";

        private ExternalModIds() {}
    }
}
