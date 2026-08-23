package dev.chise.chisetweaks.feature.resource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ResourceReloadCoordinatorSustainedTest {
    private static final List<String> OFF = List.of("vanilla");
    private static final List<String> CHEST = List.of("vanilla", "chest");
    private static final List<String> BOTH = List.of("vanilla", "chest", "concrete");

    @Test
    void tenThousandReloadCyclesNeverLeaveCoordinatorStuck() {
        ResourceReloadCoordinator coordinator = new ResourceReloadCoordinator();
        List<String> active = OFF;

        for (int cycle = 0; cycle < 10_000; cycle++) {
            List<String> target = cycle % 2 == 0 ? CHEST : BOTH;
            coordinator.begin(active, target);
            if (cycle % 3 == 0) {
                coordinator.markPending(cycle % 6 == 0 ? OFF : BOTH);
            }

            ResourceReloadCoordinator.Completion completion = coordinator.complete(cycle % 17 != 0);
            assertNotNull(completion);
            active = completion.activeSelection();

            if (completion.action() == ResourceReloadCoordinator.Action.RELOAD) {
                coordinator.begin(completion.activeSelection(), completion.targetSelection());
                ResourceReloadCoordinator.Completion followUp = coordinator.complete(true);
                assertNotNull(followUp);
                active = followUp.activeSelection();
            }

            assertFalse(coordinator.snapshot().inFlight(), "cycle=" + cycle);
            assertFalse(coordinator.snapshot().pending(), "cycle=" + cycle);
        }
    }
}
