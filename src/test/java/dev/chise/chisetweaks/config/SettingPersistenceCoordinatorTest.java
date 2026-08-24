package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingPersistenceCoordinatorTest {
    @Test
    void savesOnlyRequestedApplyManagedDomains() {
        AtomicInteger featureCalls = new AtomicInteger();
        AtomicInteger localCalls = new AtomicInteger();
        SettingPersistenceCoordinator coordinator = new SettingPersistenceCoordinator(
                () -> {
                    featureCalls.incrementAndGet();
                    return true;
                },
                () -> {
                    localCalls.incrementAndGet();
                    return true;
                });

        var result = coordinator.save(Set.of(
                SettingPersistence.FEATURE_CONFIG,
                SettingPersistence.EXTERNAL));

        assertTrue(result.successful());
        assertEquals(Set.of(), result.failedDomains());
        assertEquals(1, featureCalls.get());
        assertEquals(0, localCalls.get());
    }

    @Test
    void oneDomainFailureDoesNotBlockOtherDomainAndOnlyFailureIsReturned() {
        AtomicInteger featureCalls = new AtomicInteger();
        AtomicInteger localCalls = new AtomicInteger();
        SettingPersistenceCoordinator coordinator = new SettingPersistenceCoordinator(
                () -> {
                    featureCalls.incrementAndGet();
                    return false;
                },
                () -> {
                    localCalls.incrementAndGet();
                    return true;
                });

        var result = coordinator.save(Set.of(
                SettingPersistence.FEATURE_CONFIG,
                SettingPersistence.LOCAL_CONFIG));

        assertEquals(Set.of(SettingPersistence.FEATURE_CONFIG), result.failedDomains());
        assertEquals(1, featureCalls.get());
        assertEquals(1, localCalls.get());
    }

    @Test
    void unexpectedPersistenceExceptionIsContainedAndOtherDomainStillSaves() {
        AtomicInteger localCalls = new AtomicInteger();
        SettingPersistenceCoordinator coordinator = new SettingPersistenceCoordinator(
                () -> {
                    throw new IllegalStateException("feature write failed");
                },
                () -> {
                    localCalls.incrementAndGet();
                    return true;
                });

        var result = coordinator.save(Set.of(
                SettingPersistence.FEATURE_CONFIG,
                SettingPersistence.LOCAL_CONFIG));

        assertEquals(Set.of(SettingPersistence.FEATURE_CONFIG), result.failedDomains());
        assertEquals(1, localCalls.get());
    }

    @Test
    void emptyOrNullRequestDoesNotWriteAnything() {
        AtomicInteger calls = new AtomicInteger();
        SettingPersistenceCoordinator coordinator = new SettingPersistenceCoordinator(
                () -> {
                    calls.incrementAndGet();
                    return true;
                },
                () -> {
                    calls.incrementAndGet();
                    return true;
                });

        assertTrue(coordinator.save(Set.of()).successful());
        assertTrue(coordinator.save(null).successful());
        assertEquals(0, calls.get());
    }
}
