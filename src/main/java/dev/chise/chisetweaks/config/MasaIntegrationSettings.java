package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.integration.masa.MasaJapaneseUiMode;

/** UI bindings for the optional integration persistence domain. */
public final class MasaIntegrationSettings {
    public static final ChiseIntegerSetting JAPANESE_UI_MODE = integer(
            "masaJapaneseUiMode", MasaJapaneseUiMode.AUTO.id(), 0, 2,
            value -> MasaJapaneseUiMode.fromId(value).label(),
            () -> config().japaneseUiMode,
            value -> config().japaneseUiMode = value);

    public static final ChiseBooleanSetting LITEMATICA_PICK_REDIRECT = bool(
            "litematicaPickRedirect", () -> config().litematicaPickRedirect,
            value -> config().litematicaPickRedirect = value);
    public static final ChiseBooleanSetting TWEAKEROO_TOOL_SWITCH_GUARD = bool(
            "tweakerooToolSwitchGuard", () -> config().tweakerooToolSwitchGuard,
            value -> config().tweakerooToolSwitchGuard = value);
    public static final ChiseBooleanSetting TWEAKEROO_PERSISTENT_GAMMA = bool(
            "tweakerooPersistentGammaOverride", () -> config().tweakerooPersistentGammaOverride,
            value -> config().tweakerooPersistentGammaOverride = value);
    public static final ChiseBooleanSetting TWEAKERMORE_AUTO_PICK_GUARD = bool(
            "tweakermoreAutoPickGuard", () -> config().tweakermoreAutoPickGuard,
            value -> config().tweakermoreAutoPickGuard = value);
    public static final ChiseBooleanSetting TWEAKERMORE_MATERIAL_REFRESH = bool(
            "tweakermoreMaterialListRefresh", () -> config().tweakermoreMaterialListRefresh,
            value -> config().tweakermoreMaterialListRefresh = value);
    public static final ChiseBooleanSetting SYNCMATICA_REMOVE_DISABLED = bool(
            "syncmaticaRemoveDisabled", () -> config().syncmaticaRemoveDisabled,
            value -> config().syncmaticaRemoveDisabled = value);
    public static final ChiseBooleanSetting SYNCMATICA_REQUIRE_SHIFT = bool(
            "syncmaticaRemoveRequireShift", () -> config().syncmaticaRemoveRequireShift,
            value -> config().syncmaticaRemoveRequireShift = value);

    private MasaIntegrationSettings() {}

    private static MasaIntegrationConfig config() {
        return MasaIntegrationConfig.getInstance();
    }

    private static ChiseBooleanSetting bool(
            String name,
            java.util.function.BooleanSupplier reader,
            java.util.function.Consumer<Boolean> writer) {
        return new SimpleBooleanSetting(
                name, false, reader, writer, SettingPersistence.INTEGRATION_CONFIG);
    }

    private static ChiseIntegerSetting integer(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            java.util.function.IntFunction<String> formatter,
            java.util.function.IntSupplier reader,
            java.util.function.IntConsumer writer) {
        return new ChiseIntegerSetting(
                name, defaultValue, minValue, maxValue,
                reader, writer, formatter, SettingPersistence.INTEGRATION_CONFIG);
    }
}
