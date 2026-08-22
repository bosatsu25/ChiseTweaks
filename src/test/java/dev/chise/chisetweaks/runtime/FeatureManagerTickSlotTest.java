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
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        assertFalse(component.isActive());
        slot.runForTick(null);

        assertEquals(1, component.ticks);
        assertTrue(component.isActive());
        assertFalse(slot.isQuarantined());
    }

    @Test
    void firstRuntimeFailureQuarantinesCleansUpAndFutureTicksAreSkipped() {
        FailingRuntimeComponent component = new FailingRuntimeComponent();
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        slot.runForTick(null);
        slot.runForTick(null);

        assertTrue(slot.isQuarantined());
        assertEquals(1, component.ticks);
        assertEquals(1, component.cleanupCalls());
    }

    @Test
    void cleanupFailureDoesNotEscapeQuarantineBoundary() {
        FailingCleanupRuntimeComponent component = new FailingCleanupRuntimeComponent();
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        assertDoesNotThrow(() -> slot.runForTick(null));
        assertTrue(slot.isQuarantined());
        assertEquals(1, component.cleanupCalls());
    }

    @Test
    void failingFeatureIsQuarantinedWithoutMutatingUserConfiguration() {
        FailingFeature feature = new FailingFeature();
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(feature);

        slot.runForTick(null);

        assertTrue(slot.isQuarantined());
        assertEquals(1, feature.quarantineCalls);
        assertTrue(feature.isEnabled());
    }

    private static final class FakeRuntimeComponent implements TickingRuntimeComponent {
        private int ticks;
        private boolean active;

        @Override public String getId() { return "fake-runtime"; }
        @Override public void init() {}
        @Override public boolean isActive() { return active; }
        @Override public void tick(Minecraft client) { ticks++; active = true; }
    }

    private static class FailingRuntimeComponent implements TickingRuntimeComponent {
        private int ticks;
        private int cleanupCalls;

        @Override public String getId() { return "failing-runtime"; }
        @Override public void init() {}
        @Override public boolean isActive() { return false; }
        @Override public void tick(Minecraft client) { ticks++; throw new IllegalStateException("boom"); }
        @Override public void onQuarantined(Minecraft client) { cleanupCalls++; }

        int cleanupCalls() { return cleanupCalls; }
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
