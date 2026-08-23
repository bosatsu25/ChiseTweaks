package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ChiseIntegerSettingStateTest {
    @Test
    void refusedBoundWriteDoesNotEmitFalseChangeNotification() {
        AtomicInteger stored = new AtomicInteger(5);
        AtomicInteger callbacks = new AtomicInteger();
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "bounded", 5, 0, 10,
                "Bounded", "境界値", "Bounded", "境界値",
                stored::get,
                ignored -> {});
        setting.setValueChangeCallback(ignored -> callbacks.incrementAndGet());

        setting.setIntegerValue(8);

        assertEquals(5, setting.getIntegerValue());
        assertEquals(0, callbacks.get());
    }

    @Test
    void effectiveBoundWriteEmitsExactlyOneNotification() {
        AtomicInteger stored = new AtomicInteger(5);
        AtomicInteger callbacks = new AtomicInteger();
        ChiseIntegerSetting setting = new ChiseIntegerSetting(
                "bounded", 5, 0, 10,
                "Bounded", "境界値", "Bounded", "境界値",
                stored::get,
                stored::set);
        setting.setValueChangeCallback(ignored -> callbacks.incrementAndGet());

        setting.setIntegerValue(8);
        setting.setIntegerValue(8);

        assertEquals(8, setting.getIntegerValue());
        assertEquals(1, callbacks.get());
    }
}
