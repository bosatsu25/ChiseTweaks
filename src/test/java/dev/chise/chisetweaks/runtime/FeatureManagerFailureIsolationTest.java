package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FeatureManagerFailureIsolationTest {
    @Test
    void recoverableFailureFromIsActiveIsQuarantinedBeforeTickRuns() {
        FakeComponent component = new FakeComponent();
        component.activeFailure = new IllegalStateException("active");
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        assertDoesNotThrow(() -> slot.runForTick(null));

        assertTrue(slot.isQuarantined());
        assertEquals(0, component.tickCalls);
    }

    @Test
    void recoverableFailureFromTickIsQuarantined() {
        FakeComponent component = new FakeComponent();
        component.active = true;
        component.tickFailure = new IllegalStateException("tick");
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        assertDoesNotThrow(() -> slot.runForTick(null));

        assertTrue(slot.isQuarantined());
        assertEquals(1, component.tickCalls);
    }

    @Test
    void fatalJvmErrorFromIsActiveIsNotHidden() {
        FakeComponent component = new FakeComponent();
        component.fatalActiveFailure = new OutOfMemoryError("fatal");
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        assertThrows(OutOfMemoryError.class, () -> slot.runForTick(null));
        assertFalse(slot.isQuarantined());
    }

    @Test
    void inactiveComponentCostsNothingUntilItNeedsOneCleanupTick() {
        FakeComponent component = new FakeComponent();
        FeatureManager.TickSlot slot = new FeatureManager.TickSlot(component);

        slot.runForTick(null);
        assertEquals(0, component.tickCalls);

        component.active = true;
        slot.runForTick(null);
        assertEquals(1, component.tickCalls);

        component.active = false;
        slot.runForTick(null);
        assertEquals(2, component.tickCalls, "active-to-inactive transition gets one cleanup tick");

        slot.runForTick(null);
        assertEquals(2, component.tickCalls, "steady inactive state does not tick");
    }

    private static final class FakeComponent implements TickingRuntimeComponent {
        boolean active;
        Error fatalActiveFailure;
        RuntimeException activeFailure;
        RuntimeException tickFailure;
        int tickCalls;

        @Override public String getId() { return "fake"; }
        @Override public void init() { }

        @Override
        public boolean isActive() {
            if (fatalActiveFailure != null) throw fatalActiveFailure;
            if (activeFailure != null) throw activeFailure;
            return active;
        }

        @Override
        public void tick(Minecraft client) {
            tickCalls++;
            if (tickFailure != null) throw tickFailure;
        }
    }
}
