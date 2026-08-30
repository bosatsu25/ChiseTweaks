package dev.chise.chisetweaks.integration.compat;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

/** Reflection-only optional Nvidium bridge. No hard dependency is introduced. */
public final class NvidiumCompatibility {
    private static Field compatibleField;
    private static Field enabledField;
    private static boolean initialized;
    private static boolean available;
    private static boolean originalCaptured;
    private static boolean originallyCompatible;
    private static boolean suppressed;

    private NvidiumCompatibility() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("nvidium")) return;
        try {
            Class<?> nvidium = Class.forName("me.cortex.nvidium.Nvidium", false,
                    NvidiumCompatibility.class.getClassLoader());
            compatibleField = nvidium.getField("IS_COMPATIBLE");
            enabledField = nvidium.getField("IS_ENABLED");
            available = compatibleField.getType() == boolean.class
                    && enabledField.getType() == boolean.class;
            if (available) {
                ChiseTweaksClient.LOGGER.info("Nvidium compatibility layer available");
            }
        } catch (ReflectiveOperationException | LinkageError failure) {
            available = false;
            ChiseTweaksClient.LOGGER.warn(
                    "Nvidium API is unavailable; World Border Fix will stay no-op after {}",
                    failure.getClass().getSimpleName());
        }
    }

    public static synchronized boolean isAvailable() {
        return available;
    }

    public static synchronized boolean isSuppressed() {
        return suppressed;
    }

    public static synchronized boolean isCurrentlyEnabled() {
        if (!available || enabledField == null) return false;
        try {
            return enabledField.getBoolean(null);
        } catch (IllegalAccessException | RuntimeException failure) {
            return false;
        }
    }

    public static synchronized boolean suppress() {
        if (!available || suppressed || compatibleField == null) return false;
        try {
            if (!originalCaptured) {
                originallyCompatible = compatibleField.getBoolean(null);
                originalCaptured = true;
            }
            if (!originallyCompatible) return false;
            compatibleField.setBoolean(null, false);
            suppressed = true;
            return true;
        } catch (IllegalAccessException | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Nvidium suppression failed after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    public static synchronized boolean restore() {
        if (!available || !suppressed || compatibleField == null) return false;
        try {
            compatibleField.setBoolean(null, originallyCompatible);
            suppressed = false;
            return true;
        } catch (IllegalAccessException | RuntimeException failure) {
            suppressed = false;
            ChiseTweaksClient.LOGGER.warn(
                    "Nvidium restore failed after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
    }
}
