package dev.chise.chisetweaks.feature.resource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceReloadCoordinatorTest {
    private static final List<String> OFF = List.of("vanilla");
    private static final List<String> CHEST_ON = List.of("vanilla", "chest");
    private static final List<String> BOTH_ON = List.of("vanilla", "chest", "concrete");

    @Test
    void rapidOnThenOffSchedulesExactlyOneFollowUpReload() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending();

        var first = coordinator.complete(OFF, true);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, first.action());
        assertEquals(CHEST_ON, first.activeSelection());
        assertEquals(OFF, first.targetSelection());
        assertFalse(coordinator.isInFlight());

        coordinator.begin(first.activeSelection(), first.targetSelection());
        var second = coordinator.complete(OFF, true);
        assertEquals(ResourceReloadCoordinator.Action.NONE, second.action());
        assertEquals(OFF, second.activeSelection());
    }

    @Test
    void rapidOnOffOnCoalescesBackToCompletedTargetWithoutExtraReload() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending();
        coordinator.markPending();

        var completion = coordinator.complete(CHEST_ON, true);
        assertEquals(ResourceReloadCoordinator.Action.NONE, completion.action());
        assertEquals(CHEST_ON, completion.activeSelection());
        assertFalse(coordinator.isInFlight());
    }

    @Test
    void changeToSecondPackDuringReloadPreservesBothDesiredSelections() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending();

        var first = coordinator.complete(BOTH_ON, true);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, first.action());
        assertEquals(CHEST_ON, first.activeSelection());
        assertEquals(BOTH_ON, first.targetSelection());
    }

    @Test
    void failedReloadWithoutFollowUpRestoresKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        var completion = coordinator.complete(CHEST_ON, false);
        assertEquals(ResourceReloadCoordinator.Action.RESTORE, completion.action());
        assertEquals(OFF, completion.activeSelection());
        assertEquals(OFF, completion.targetSelection());
    }

    @Test
    void failedReloadWithNewDesiredStateRetriesFromKnownActiveSelection() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        coordinator.markPending();
        var completion = coordinator.complete(BOTH_ON, false);
        assertEquals(ResourceReloadCoordinator.Action.RELOAD, completion.action());
        assertEquals(OFF, completion.activeSelection());
        assertEquals(BOTH_ON, completion.targetSelection());
    }

    @Test
    void resetClearsInFlightState() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        coordinator.begin(OFF, CHEST_ON);
        assertTrue(coordinator.isInFlight());
        coordinator.reset();
        assertFalse(coordinator.isInFlight());
    }
}
