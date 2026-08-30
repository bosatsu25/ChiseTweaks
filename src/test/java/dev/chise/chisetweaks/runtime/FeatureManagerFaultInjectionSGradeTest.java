package dev.chise.chisetweaks.runtime;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S5: inject recoverable runtime failures and prove component-local quarantine/cleanup semantics. */
final class FeatureManagerFaultInjectionSGradeTest {

    @Test
    void tickFailureQuarantinesOnlyTheFailingSlotAndRunsCleanupOnce() {
        FaultyComponent failing = new FaultyComponent("failing", FailureStage.TICK, false);
        HealthyComponent healthy = new HealthyComponent("healthy");
        FeatureManager.ComponentSlot failingSlot = new FeatureManager.ComponentSlot(failing);
        FeatureManager.ComponentSlot healthySlot = new FeatureManager.ComponentSlot(healthy);

        failingSlot.runForTick(null);
        healthySlot.runForTick(null);
        healthySlot.runForTick(null);

        assertTrue(failingSlot.isQuarantined());
        assertEquals(1, failing.cleanupCalls);
        assertFalse(healthySlot.isQuarantined());
        assertEquals(2, healthy.tickCalls);
    }

    @Test
    void sessionResetFailureQuarantinesAndCleansUp() {
        FaultyComponent failing = new FaultyComponent("session", FailureStage.SESSION, false);
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(failing);

        slot.resetSession(null);
        slot.resetSession(null);

        assertTrue(slot.isQuarantined());
        assertEquals(1, failing.sessionCalls);
        assertEquals(1, failing.cleanupCalls);
    }

    @Test
    void linkageErrorIsContainedAtTheRuntimeBoundary() {
        FaultyComponent failing = new FaultyComponent("linkage", FailureStage.TICK, true);
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(failing);

        slot.runForTick(null);

        assertTrue(slot.isQuarantined());
        assertEquals(1, failing.cleanupCalls);
    }

    @Test
    void cleanupFailureCannotEscapeQuarantine() {
        FaultyCleanupComponent failing = new FaultyCleanupComponent();
        FeatureManager.ComponentSlot slot = new FeatureManager.ComponentSlot(failing);

        slot.quarantineDuringInitialization(null, new IllegalStateException("injected"));

        assertTrue(slot.isQuarantined());
        assertEquals(1, failing.cleanupCalls);
    }

    private enum FailureStage { TICK, SESSION }

    private static final class HealthyComponent implements TickingRuntimeComponent {
        private final String id;
        private int tickCalls;

        private HealthyComponent(String id) { this.id = id; }
        @Override public String getId() { return id; }
        @Override public void init() {}
        @Override public void tick(Minecraft client) { tickCalls++; }
    }

    private static final class FaultyComponent
            implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
        private final String id;
        private final FailureStage stage;
        private final boolean linkage;
        private int sessionCalls;
        private int cleanupCalls;

        private FaultyComponent(String id, FailureStage stage, boolean linkage) {
            this.id = id;
            this.stage = stage;
            this.linkage = linkage;
        }

        @Override public String getId() { return id; }
        @Override public void init() {}

        @Override
        public void tick(Minecraft client) {
            if (stage != FailureStage.TICK) return;
            if (linkage) throw new NoClassDefFoundError("injected-linkage");
            throw new IllegalStateException("injected-tick");
        }

        @Override
        public void resetSession(Minecraft client) {
            sessionCalls++;
            if (stage == FailureStage.SESSION) throw new IllegalStateException("injected-session");
        }

        @Override public void onQuarantined(Minecraft client) { cleanupCalls++; }
    }

    private static final class FaultyCleanupComponent implements RuntimeComponent {
        private int cleanupCalls;
        @Override public String getId() { return "cleanup"; }
        @Override public void init() {}
        @Override public void onQuarantined(Minecraft client) {
            cleanupCalls++;
            throw new IllegalStateException("injected-cleanup");
        }
    }
}
