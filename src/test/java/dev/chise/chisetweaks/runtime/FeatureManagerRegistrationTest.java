package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.feature.Feature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class FeatureManagerRegistrationTest {
    @Test
    void featureAndRuntimeComponentIdsShareOneNamespace() throws Exception {
        FeatureManager manager = newManager();
        manager.registerFeature(new FakeFeature("shared-id"));

        assertThrows(
                IllegalStateException.class,
                () -> manager.registerRuntimeComponent(new FakeRuntimeComponent("shared-id")));
    }

    @Test
    void runtimeComponentAndFeatureIdsShareOneNamespace() throws Exception {
        FeatureManager manager = newManager();
        manager.registerRuntimeComponent(new FakeRuntimeComponent("shared-id"));

        assertThrows(
                IllegalStateException.class,
                () -> manager.registerFeature(new FakeFeature("shared-id")));
    }

    @Test
    void componentIdsRejectSurroundingWhitespaceInsteadOfSilentlyNormalizing() throws Exception {
        FeatureManager manager = newManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerFeature(new FakeFeature(" spaced-id ")));
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerRuntimeComponent(new FakeRuntimeComponent(" spaced-id ")));
    }

    private static FeatureManager newManager() throws Exception {
        Constructor<FeatureManager> constructor = FeatureManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static final class FakeFeature implements Feature {
        private final String id;
        private boolean enabled = true;

        private FakeFeature(String id) {
            this.id = id;
        }

        @Override public String getId() { return id; }
        @Override public String getName() { return "Fake feature"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    private record FakeRuntimeComponent(String id) implements RuntimeComponent {
        @Override public String getId() { return id; }
        @Override public void init() {}
        @Override public boolean isActive() { return true; }
    }
}
