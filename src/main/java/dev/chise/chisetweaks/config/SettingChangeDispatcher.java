package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.ChiseTweaksClient;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** 値変更後の副作用callbackをfail-softで実行し、設定値そのものの更新を巻き戻さない。 */
public final class SettingChangeDispatcher {
    private static final AtomicLong REVISION = new AtomicLong();

    private SettingChangeDispatcher() {}

    public static long revision() {
        return REVISION.get();
    }

    public static void markChanged() {
        REVISION.incrementAndGet();
    }

    static <T> void notifySafely(String settingName, T setting, Consumer<T> callback) {
        if (callback == null) return;
        try {
            callback.accept(setting);
        } catch (RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Setting '{}' change callback failed after {}; value remains applied",
                    safeName(settingName),
                    failure.getClass().getSimpleName());
        }
    }

    private static String safeName(String value) {
        String normalized = Objects.requireNonNullElse(value, "unknown").trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
