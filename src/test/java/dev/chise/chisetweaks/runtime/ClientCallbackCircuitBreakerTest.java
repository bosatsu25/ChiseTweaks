package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

final class ClientCallbackCircuitBreakerTest {
    @BeforeEach
    void resetBreaker() {
        ClientCallbackCircuitBreaker.resetSessionState();
    }

    @Test
    void recoverableCallbackFailureOpensOnlyThatCallback() {
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_HUD_RENDER,
                () -> { throw new IllegalStateException("test"); });

        assertTrue(ClientCallbackCircuitBreaker.isOpen(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_HUD_RENDER));
        assertFalse(ClientCallbackCircuitBreaker.isOpen(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER));
        assertEquals(1, ClientCallbackCircuitBreaker.openCount());
    }

    @Test
    void newSessionClearsCallbackQuarantine() {
        ClientCallbackCircuitBreaker.trip(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER,
                new LinkageError("test"));
        assertEquals(1, ClientCallbackCircuitBreaker.openCount());

        ClientCallbackCircuitBreaker.resetSessionState();

        assertEquals(0, ClientCallbackCircuitBreaker.openCount());
    }

    @Test
    void argumentVariantReusesConsumerAndPassesTheOriginalValue() {
        AtomicReference<String> observed = new AtomicReference<>();
        Consumer<String> consumer = observed::set;

        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_HUD_RENDER,
                "value",
                consumer);

        assertEquals("value", observed.get());
        assertEquals(0, ClientCallbackCircuitBreaker.openCount());
    }

    @Test
    void fatalJvmErrorsAreRethrownAndDoNotBecomeRecoverableQuarantine() {
        assertThrows(OutOfMemoryError.class, () -> ClientCallbackCircuitBreaker.trip(
                ClientCallbackCircuitBreaker.Callback.SESSION_FEATURE_RESET,
                new OutOfMemoryError("fatal")));
        assertEquals(0, ClientCallbackCircuitBreaker.openCount());
    }
}
