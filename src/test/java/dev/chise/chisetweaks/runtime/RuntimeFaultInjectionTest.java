package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeFaultInjectionTest {
    @Test
    void linkageErrorDuringTickQuarantinesOnlyTheFailingComponent() {
        LinkageTickComponent failing = new LinkageTickComponent();
        HealthyTickComponent healthy = new HealthyTickComponent();
        FeatureManager.ComponentSlot failingSlot = new FeatureManager.ComponentSlot(failing);
        FeatureManager.ComponentSlot healthySlot = new FeatureManager.ComponentSlot(healthy);

        assertDoesNotThrow(() -> failingSlot.runForTick(null));
        assertDoesNotThrow(() -> healthySlot.runForTick(null));
        assertDoesNotThrow(() -> healthySlot.runForTick(null));

        assertTrue(failingSlot.isQuarantined());
        assertFalse(healthySlot.isQuarantined());
        assertEquals(1, failing.ticks);
        assertEquals(1, failing.cleanups);
        assertEquals(2, healthy.ticks);
    }

    @Test
    void linkageErrorDuringSessionResetQuarantinesBeforeFutureTicks() {
        LinkageSessionComponent component = new LinkageSessionComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        assertDoesNotThrow(() -> slot.resetSession(null));
        assertDoesNotThrow(() -> slot.runForTick(null));

        assertTrue(slot.isQuarantined());
        assertEquals(0, component.ticks);
        assertEquals(1, component.cleanups);
    }

    @Test
    void cleanupLinkageErrorNeverEscapesTheQuarantineBoundary() {
        CleanupLinkageComponent component = new CleanupLinkageComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        assertDoesNotThrow(() -> slot.runForTick(null));

        assertTrue(slot.isQuarantined());
        assertEquals(1, component.cleanupAttempts);
    }

    @Test
    void linkageErrorDuringInitializationRemovesAllSchedules() throws Exception {
        FeatureManager manager = newManager();
        InitLinkageComponent component = new InitLinkageComponent();
        manager.registerComponent(component);
        FeatureManager.ComponentSlot slot = componentSlot(manager, component.getId());

        assertDoesNotThrow(() -> initialize(manager, slot));

        assertTrue(slot.isQuarantined());
        assertEquals(1, component.cleanups);
        assertEquals(0, pendingTickSlots(manager).size());
        assertEquals(0, pendingSessionSlots(manager).size());
    }

    private static FeatureManager newManager() throws Exception {
        Constructor<FeatureManager> constructor = FeatureManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void initialize(FeatureManager manager, FeatureManager.ComponentSlot slot) {
        try {
            Method method = FeatureManager.class.getDeclaredMethod(
                    "initializeComponent", FeatureManager.ComponentSlot.class);
            method.setAccessible(true);
            method.invoke(manager, slot);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static FeatureManager.ComponentSlot componentSlot(FeatureManager manager, String id) throws Exception {
        Field field = FeatureManager.class.getDeclaredField("componentSlots");
        field.setAccessible(true);
        Map<String, FeatureManager.ComponentSlot> slots =
                (Map<String, FeatureManager.ComponentSlot>) field.get(manager);
        return slots.get(id);
    }

    @SuppressWarnings("unchecked")
    private static List<FeatureManager.ComponentSlot> pendingTickSlots(FeatureManager manager) throws Exception {
        Field field = FeatureManager.class.getDeclaredField("mutableTickSlots");
        field.setAccessible(true);
        return (List<FeatureManager.ComponentSlot>) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private static List<FeatureManager.ComponentSlot> pendingSessionSlots(FeatureManager manager) throws Exception {
        Field field = FeatureManager.class.getDeclaredField("mutableSessionSlots");
        field.setAccessible(true);
        return (List<FeatureManager.ComponentSlot>) field.get(manager);
    }

    private static class HealthyTickComponent implements TickingRuntimeComponent {
        int ticks;
        @Override public String getId() { return "healthy-fault-injection"; }
        @Override public void init() {}
        @Override public void tick(Minecraft client) { ticks++; }
    }

    private static class LinkageTickComponent implements TickingRuntimeComponent {
        int ticks;
        int cleanups;
        @Override public String getId() { return "linkage-tick"; }
        @Override public void init() {}
        @Override public void tick(Minecraft client) {
            ticks++;
            throw new NoClassDefFoundError("fault-injection");
        }
        @Override public void onQuarantined(Minecraft client) { cleanups++; }
    }

    private static final class LinkageSessionComponent extends LinkageTickComponent
            implements SessionAwareRuntimeComponent {
        @Override public String getId() { return "linkage-session"; }
        @Override public void resetSession(Minecraft client) {
            throw new NoClassDefFoundError("session-fault-injection");
        }
    }

    private static final class CleanupLinkageComponent extends LinkageTickComponent {
        int cleanupAttempts;
        @Override public String getId() { return "cleanup-linkage"; }
        @Override public void onQuarantined(Minecraft client) {
            cleanupAttempts++;
            throw new NoClassDefFoundError("cleanup-fault-injection");
        }
    }

    private static final class InitLinkageComponent extends LinkageTickComponent
            implements SessionAwareRuntimeComponent {
        @Override public String getId() { return "init-linkage"; }
        @Override public void init() {
            throw new NoClassDefFoundError("init-fault-injection");
        }
        @Override public void resetSession(Minecraft client) {}
    }
}
