package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.feature.TickingFeature;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureManagerTickSlotTest {
    @Test
    void initiallyInactiveRuntimeComponentStillGetsOpportunityToActivate() {
        FakeRuntimeComponent component = new FakeRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);
        assertTrue(slot.isTicking());
        assertFalse(slot.isSessionAware());
        assertEquals("fake-runtime", slot.componentId());
        assertFalse(component.isActive());
        slot.runForTick(null);
        assertEquals(1, component.ticks);
        assertTrue(component.isActive());
        assertFalse(slot.isQuarantined());
    }

    @Test
    void plainRuntimeComponentSkipsUnsupportedTickAndSessionLifecycles() {
        PlainRuntimeComponent component = new PlainRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        assertFalse(slot.isTicking());
        assertFalse(slot.isSessionAware());
        assertEquals("plain-runtime", slot.componentId());
        assertDoesNotThrow(() -> slot.runForTick(null));
        assertDoesNotThrow(() -> slot.resetSession(null));
        assertFalse(slot.isQuarantined());
    }

    @Test
    void sessionAwareComponentExposesCapabilityAndResetsWithoutQuarantine() {
        SessionOnlyRuntimeComponent component = new SessionOnlyRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        assertFalse(slot.isTicking());
        assertTrue(slot.isSessionAware());
        slot.resetSession(null);

        assertEquals(1, component.resets);
        assertFalse(slot.isQuarantined());
    }

    @Test
    void initializeDelegatesToRuntimeComponent() {
        PlainRuntimeComponent component = new PlainRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        slot.initialize();

        assertEquals(1, component.initializations);
    }

    @Test
    void firstRuntimeFailureQuarantinesCleansUpAndFutureTicksAreSkipped() {
        FailingRuntimeComponent component = new FailingRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);
        slot.runForTick(null);
        slot.runForTick(null);
        assertTrue(slot.isQuarantined());
        assertEquals(1, component.ticks);
        assertEquals(1, component.cleanupCalls());
    }

    @Test
    void lifecycleFailureQuarantinesBeforeAnyFutureTickCanUseStaleState() {
        FailingSessionRuntimeComponent component = new FailingSessionRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);

        slot.resetSession(null);
        slot.runForTick(null);

        assertTrue(slot.isQuarantined());
        assertEquals(0, component.ticks);
        assertEquals(1, component.cleanupCalls());
    }

    @Test
    void oneFailingSlotDoesNotQuarantineOrStopAnotherSlot() {
        FailingRuntimeComponent failing = new FailingRuntimeComponent();
        FakeRuntimeComponent healthy = new FakeRuntimeComponent();
        FeatureManager.ComponentSlot failingSlot = new FeatureManager.ComponentSlot(failing);
        FeatureManager.ComponentSlot healthySlot = new FeatureManager.ComponentSlot(healthy);

        failingSlot.runForTick(null);
        healthySlot.runForTick(null);
        failingSlot.runForTick(null);
        healthySlot.runForTick(null);

        assertTrue(failingSlot.isQuarantined());
        assertFalse(healthySlot.isQuarantined());
        assertEquals(1, failing.ticks);
        assertEquals(2, healthy.ticks);
    }

    @Test
    void cleanupFailureDoesNotEscapeQuarantineBoundary() {
        FailingCleanupRuntimeComponent component = new FailingCleanupRuntimeComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(component);
        assertDoesNotThrow(() -> slot.runForTick(null));
        assertTrue(slot.isQuarantined());
        assertEquals(1, component.cleanupCalls());
    }

    @Test
    void failingFeatureIsQuarantinedWithoutMutatingUserConfiguration() {
        FailingFeature feature = new FailingFeature();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(feature);
        slot.runForTick(null);
        assertTrue(slot.isQuarantined());
        assertEquals(1, feature.quarantineCalls);
        assertTrue(feature.isEnabled());
    }

    private static final class PlainRuntimeComponent implements RuntimeComponent {
        private int initializations;

        @Override public String getId() { return "plain-runtime"; }
        @Override public void init() { initializations++; }
    }

    private static final class SessionOnlyRuntimeComponent
            implements RuntimeComponent, SessionAwareRuntimeComponent {
        private int resets;

        @Override public String getId() { return "session-runtime"; }
        @Override public void init() {}
        @Override public void resetSession(Minecraft client) { resets++; }
    }

    private static final class FakeRuntimeComponent implements TickingRuntimeComponent {
        private int ticks;
        private boolean active;

        @Override public String getId() { return "fake-runtime"; }
        @Override public void init() {}
        @Override public void tick(Minecraft client) { ticks++; active = true; }
        boolean isActive() { return active; }
    }

    private static class FailingRuntimeComponent implements TickingRuntimeComponent {
        protected int ticks;
        private int cleanupCalls;

        @Override public String getId() { return "failing-runtime"; }
        @Override public void init() {}
        @Override public void tick(Minecraft client) { ticks++; throw new IllegalStateException("boom"); }
        @Override public void onQuarantined(Minecraft client) { cleanupCalls++; }
        int cleanupCalls() { return cleanupCalls; }
    }

    private static final class FailingSessionRuntimeComponent extends FailingRuntimeComponent
            implements SessionAwareRuntimeComponent {
        @Override public String getId() { return "failing-session-runtime"; }
        @Override public void resetSession(Minecraft client) { throw new IllegalStateException("reset"); }
    }

    private static final class FailingCleanupRuntimeComponent extends FailingRuntimeComponent {
        @Override public String getId() { return "failing-cleanup-runtime"; }
        @Override public void onQuarantined(Minecraft client) {
            super.onQuarantined(client);
            throw new IllegalStateException("cleanup");
        }
    }

    private static final class FailingFeature implements TickingFeature {
        private int quarantineCalls;

        @Override public String getId() { return "failing-feature"; }
        @Override public String getName() { return "Failing feature"; }
        @Override public boolean isEnabled() { return true; }
        @Override public void tick(Minecraft client) { throw new IllegalStateException("boom"); }
        @Override public void onQuarantined(Minecraft client) { quarantineCalls++; }
    }
}
