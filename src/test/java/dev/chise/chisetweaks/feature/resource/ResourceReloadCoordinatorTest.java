package dev.chise.chisetweaks.feature.resource;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceReloadCoordinatorTest {
    private static final List<String> OFF = List.of("vanilla");
    private static final List<String> CHEST_ON = List.of("vanilla", "chest");
    private static final List<String> BOTH_ON = List.of("vanilla", "chest", "concrete");

    @Test
    void rapidOnThenOffSchedulesExactlyOneFollowUpReload() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(OFF);

        var first = coordinator.complete(true);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, first.action());
        assertEquals(CHEST_ON, first.activeSelection());
        assertEquals(OFF, first.targetSelection());
        assertFalse(coordinator.isInFlight());

        coordinator.begin(first.activeSelection(), first.targetSelection());
        var second = coordinator.complete(true);
        assertEquals(ResourceReloadCoordinator.Action.NONE, second.action());
        assertEquals(OFF, second.activeSelection());
    }

    @Test
    void rapidOnOffOnCoalescesBackToCompletedTargetWithoutExtraReload() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(OFF);
        coordinator.markPending(CHEST_ON);

        var completion = coordinator.complete(true);
        assertEquals(ResourceReloadCoordinator.Action.NONE, completion.action());
        assertEquals(CHEST_ON, completion.activeSelection());
        assertFalse(coordinator.isInFlight());
    }

    @Test
    void changeToSecondPackDuringReloadPreservesBothDesiredSelections() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(BOTH_ON);

        var first = coordinator.complete(true);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, first.action());
        assertEquals(CHEST_ON, first.activeSelection());
        assertEquals(BOTH_ON, first.targetSelection());
    }

    @Test
    void failedReloadWithoutFollowUpRestoresKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        var completion = coordinator.complete(false);
        assertEquals(ResourceReloadCoordinator.Action.RESTORE, completion.action());
        assertEquals(OFF, completion.activeSelection());
        assertEquals(OFF, completion.targetSelection());
    }

    @Test
    void failedReloadWithNewDesiredStateRetriesFromKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(BOTH_ON);
        var completion = coordinator.complete(false);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, completion.action());
        assertEquals(OFF, completion.activeSelection());
        assertEquals(BOTH_ON, completion.targetSelection());
    }

    @Test
    void clientThreadSchedulingFailureCapturesRecoverableDesiredState() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(BOTH_ON);
        var recovery = coordinator.terminalFailure(true);
        assertEquals(CHEST_ON, recovery.activeSelection());
        assertEquals(BOTH_ON, recovery.desiredSelection());
        assertTrue(recovery.requiresReload());
        assertFalse(coordinator.isInFlight());
    }

    @Test
    void failedReloadAndSchedulingFailureFallsBackToLastKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(BOTH_ON);
        var recovery = coordinator.terminalFailure(false);
        assertEquals(OFF, recovery.activeSelection());
        assertEquals(BOTH_ON, recovery.desiredSelection());
        assertTrue(recovery.requiresReload());
        assertFalse(coordinator.isInFlight());
    }

    @Test
    void terminalFailureWithoutPendingChangeNeedsNoAdditionalReloadAfterSuccess() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        var recovery = coordinator.terminalFailure(true);
        assertEquals(CHEST_ON, recovery.activeSelection());
        assertEquals(CHEST_ON, recovery.desiredSelection());
        assertFalse(recovery.requiresReload());
    }

    @Test
    void disconnectDuringReloadRecoversFromLastKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        var recovery = coordinator.cancel(BOTH_ON);
        assertEquals(OFF, recovery.activeSelection());
        assertEquals(BOTH_ON, recovery.desiredSelection());
        assertTrue(recovery.requiresReload());
        assertFalse(coordinator.isInFlight());
    }

    @Test
    void lateCompletionAfterDisconnectIsIgnoredInsteadOfThrowing() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.cancel(CHEST_ON);
        assertNull(coordinator.complete(true));
        assertNull(coordinator.terminalFailure(true));
        assertNull(coordinator.cancel(OFF));
    }

    @Test
    void pendingChangeWhileIdleIsIgnored() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.markPending(CHEST_ON);
        assertFalse(coordinator.snapshot().inFlight());
        assertFalse(coordinator.snapshot().pending());
        assertNull(coordinator.complete(true));
    }

    @Test
    void secondBeginWhileReloadIsInFlightIsRejected() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        assertThrows(IllegalStateException.class, () -> coordinator.begin(CHEST_ON, BOTH_ON));
        assertTrue(coordinator.isInFlight());
    }

    @Test
    void coordinatorDefensivelyCopiesAllSelections() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        ArrayList<String> fallback = new ArrayList<>(OFF);
        ArrayList<String> target = new ArrayList<>(CHEST_ON);
        coordinator.begin(fallback, target);
        fallback.add("mutated-fallback");
        target.add("mutated-target");

        ArrayList<String> pending = new ArrayList<>(BOTH_ON);
        coordinator.markPending(pending);
        pending.add("mutated-pending");
        var completion = coordinator.complete(true);

        assertEquals(CHEST_ON, completion.activeSelection());
        assertEquals(BOTH_ON, completion.targetSelection());
        assertThrows(UnsupportedOperationException.class,
                () -> completion.activeSelection().add("mutation"));
    }

    @Test
    void snapshotReportsPendingStateAndResetClearsIt() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending(BOTH_ON);
        assertTrue(coordinator.snapshot().inFlight());
        assertTrue(coordinator.snapshot().pending());
        coordinator.reset();
        assertFalse(coordinator.snapshot().inFlight());
        assertFalse(coordinator.snapshot().pending());
    }
}
