package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FeatureManagerInitializationTest {
    @Test
    void failedInitializationRemovesComponentFromSessionSchedule() throws Exception {
        FeatureManager manager = newManager();
        FailingSessionComponent component = new FailingSessionComponent();
        manager.registerComponent(component);

        initialize(manager, component);

        assertEquals(0, pendingSessionComponents(manager).size());
    }

    private static FeatureManager newManager() throws Exception {
        Constructor<FeatureManager> constructor = FeatureManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void initialize(FeatureManager manager, RuntimeComponent component) throws Exception {
        Method method = FeatureManager.class.getDeclaredMethod("initializeComponent", RuntimeComponent.class);
        method.setAccessible(true);
        method.invoke(manager, component);
    }

    @SuppressWarnings("unchecked")
    private static List<SessionAwareRuntimeComponent> pendingSessionComponents(FeatureManager manager)
            throws Exception {
        Field field = FeatureManager.class.getDeclaredField("mutableSessionComponents");
        field.setAccessible(true);
        return (List<SessionAwareRuntimeComponent>) field.get(manager);
    }

    private static final class FailingSessionComponent
            implements RuntimeComponent, SessionAwareRuntimeComponent {
        @Override public String getId() { return "failing-session"; }
        @Override public void init() { throw new IllegalStateException("boom"); }
        @Override public boolean isActive() { return false; }
        @Override public void resetSession(Minecraft client) {}
    }
}
