package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureManagerInitializationTest {
    @Test
    void failedInitializationRemovesSessionScheduleAndUsesCommonCleanup() throws Exception {
        FeatureManager manager = newManager();
        FailingSessionComponent component = new FailingSessionComponent();
        manager.registerComponent(component);
        FeatureManager.ComponentSlot slot = componentSlot(manager, component.getId());

        initialize(manager, slot);

        assertEquals(0, pendingSessionSlots(manager).size());
        assertTrue(slot.isQuarantined());
        assertEquals(1, component.cleanupCalls);
    }

    private static FeatureManager newManager() throws Exception {
        Constructor<FeatureManager> constructor = FeatureManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void initialize(FeatureManager manager, FeatureManager.ComponentSlot slot) throws Exception {
        Method method = FeatureManager.class.getDeclaredMethod(
                "initializeComponent", FeatureManager.ComponentSlot.class);
        method.setAccessible(true);
        method.invoke(manager, slot);
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
    private static List<FeatureManager.ComponentSlot> pendingSessionSlots(FeatureManager manager)
            throws Exception {
        Field field = FeatureManager.class.getDeclaredField("mutableSessionSlots");
        field.setAccessible(true);
        return (List<FeatureManager.ComponentSlot>) field.get(manager);
    }

    private static final class FailingSessionComponent
            implements RuntimeComponent, SessionAwareRuntimeComponent {
        private int cleanupCalls;

        @Override public String getId() { return "failing-session"; }
        @Override public void init() { throw new IllegalStateException("boom"); }
        @Override public void resetSession(Minecraft client) {}
        @Override public void onQuarantined(Minecraft client) { cleanupCalls++; }
    }
}
