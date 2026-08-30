package dev.chise.chisetweaks.config;

/** UI bindings for optional renderer/runtime compatibility integrations. */
public final class CompatibilityIntegrationSettings {
    public static final ChiseBooleanSetting WORLD_BORDER_FIX_ENABLED = bool(
            "worldBorderFixEnabled", () -> config().worldBorderFixEnabled,
            value -> config().worldBorderFixEnabled = value);
    public static final ChiseBooleanSetting WORLD_BORDER_FIX_XRAY = bool(
            "worldBorderFixXray", () -> config().worldBorderFixXray,
            value -> config().worldBorderFixXray = value);
    public static final ChiseIntegerSetting WORLD_BORDER_FIX_DISTANCE = integer(
            "worldBorderFixDistance", 128, 1, 8192,
            value -> value + " blocks",
            () -> config().worldBorderFixDistance,
            value -> config().worldBorderFixDistance = value);
    public static final ChiseBooleanSetting WORLD_BORDER_FIX_FAR_COORDS = bool(
            "worldBorderFixFarCoords", () -> config().worldBorderFixFarCoords,
            value -> config().worldBorderFixFarCoords = value);
    public static final ChiseIntegerSetting WORLD_BORDER_FIX_COORD_THRESHOLD = integer(
            "worldBorderFixCoordThreshold", 100000, 1000, 29999999,
            value -> Integer.toString(value),
            () -> config().worldBorderFixCoordThreshold,
            value -> config().worldBorderFixCoordThreshold = value);
    public static final ChiseBooleanSetting WORLD_BORDER_FIX_AUTO_REENABLE = bool(
            "worldBorderFixAutoReenable", () -> config().worldBorderFixAutoReenable,
            value -> config().worldBorderFixAutoReenable = value);

    private CompatibilityIntegrationSettings() {}

    private static CompatibilityIntegrationConfig config() {
        return CompatibilityIntegrationConfig.getInstance();
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
