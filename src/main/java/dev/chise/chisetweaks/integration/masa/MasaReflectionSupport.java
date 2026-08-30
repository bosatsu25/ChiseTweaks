package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Small fail-soft reflection boundary for optional external APIs that are not compile-time dependencies. */
public final class MasaReflectionSupport {
    private MasaReflectionSupport() {}

    public static void restoreTweakerooGammaIfConfigured() {
        if (!MasaIntegrationConfig.getInstance().tweakerooPersistentGammaOverride
                || !MasaModAvailability.isLoaded(MasaModAvailability.TWEAKEROO)) return;
        try {
            Class<?> featureToggle = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
            Field field = featureToggle.getField("TWEAK_GAMMA_OVERRIDE");
            Object toggle = field.get(null);
            Method changed = toggle.getClass().getMethod("onValueChanged");
            changed.invoke(toggle);
        } catch (ReflectiveOperationException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.debug(
                    "Tweakeroo gamma integration unavailable after {}",
                    failure.getClass().getSimpleName());
        }
    }

    public static void refreshTweakerMoreMaterialListIfConfigured() {
        if (!MasaIntegrationConfig.getInstance().tweakermoreMaterialListRefresh
                || !MasaModAvailability.isLoaded(MasaModAvailability.TWEAKERMORE)
                || !MasaModAvailability.isLoaded(MasaModAvailability.LITEMATICA)) return;
        try {
            Class<?> dataManager = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Method getter = dataManager.getMethod("getMaterialList");
            Object materialList = getter.invoke(null);
            if (materialList == null) return;
            Method refresh = materialList.getClass().getMethod("refreshPreFilteredList");
            refresh.invoke(materialList);
        } catch (ReflectiveOperationException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.debug(
                    "TweakerMore material-list integration unavailable after {}",
                    failure.getClass().getSimpleName());
        }
    }

    public static boolean isSyncmaticaRemoveListener(Object listener) {
        if (listener == null) return false;
        try {
            Field type = findField(listener.getClass(), "type");
            if (type == null) return false;
            type.setAccessible(true);
            Object value = type.get(listener);
            return value != null && "REMOVE".equals(value.toString());
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
