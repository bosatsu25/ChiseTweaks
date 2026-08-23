package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseIntegerSettingStateTest {
    @Test
    void refusedBoundWriteDoesNotEmitFalseChangeNotification() {
        AtomicInteger stored = new AtomicInteger(5);
        AtomicInteger callbacks = new AtomicInteger();
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "bounded", 5, 0, 10,
                "Bounded", "境界値", "Bounded", "境界値",
                stored::get,
                ignored -> {},
                SettingPersistence.LOCAL_CONFIG);
        setting.setValueChangeCallback(ignored -> callbacks.incrementAndGet());

        assertFalse(setting.setIntegerValue(8));

        assertEquals(5, setting.getIntegerValue());
        assertEquals(0, callbacks.get());
        assertEquals(SettingPersistence.LOCAL_CONFIG, setting.persistence());
    }

    @Test
    void effectiveBoundWriteEmitsExactlyOneNotification() {
        AtomicInteger stored = new AtomicInteger(5);
        AtomicInteger callbacks = new AtomicInteger();
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "bounded", 5, 0, 10,
                "Bounded", "境界値", "Bounded", "境界値",
                stored::get,
                stored::set,
                SettingPersistence.LOCAL_CONFIG);
        setting.setValueChangeCallback(ignored -> callbacks.incrementAndGet());

        assertTrue(setting.setIntegerValue(8));
        assertFalse(setting.setIntegerValue(8));

        assertEquals(8, setting.getIntegerValue());
        assertEquals(1, callbacks.get());
        assertEquals(SettingPersistence.LOCAL_CONFIG, setting.persistence());
    }

    @Test
    void callbackFailureDoesNotUndoEffectiveIntegerChange() {
        AtomicInteger stored = new AtomicInteger(5);
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "bounded", 5, 0, 10,
                "Bounded", "境界値", "Bounded", "境界値",
                stored::get,
                stored::set,
                SettingPersistence.LOCAL_CONFIG);
        setting.setValueChangeCallback(ignored -> {
            throw new IllegalStateException("refresh failed");
        });

        assertTrue(setting.setIntegerValue(9));
        assertEquals(9, setting.getIntegerValue());
    }

    @Test
    void unboundIntegerDefaultsToFeaturePersistence() {
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "featureInteger", 2, 0, 10,
                "Feature integer", "機能整数", "Feature integer", "機能整数");
        assertEquals(SettingPersistence.FEATURE_CONFIG, setting.persistence());
    }
}
