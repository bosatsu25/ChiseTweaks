package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import dev.chise.chisetweaks.feature.rendering.AncientDebrisAnalyzerFeature;
import dev.chise.chisetweaks.feature.rendering.LavaHighlightFeature;
import dev.chise.chisetweaks.feature.rendering.worksite.WorksiteVisibilityEngine;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FeatureManager {
    private static final FeatureManager INSTANCE = new FeatureManager();
    private static final TickSlot[] NO_TICK_SLOTS = new TickSlot[0];
    private static final SessionAwareRuntimeComponent[] NO_SESSION_COMPONENTS =
            new SessionAwareRuntimeComponent[0];

    private final Map<String, RuntimeComponent> components = new LinkedHashMap<>();
    private final List<TickSlot> mutableTickSlots = new ArrayList<>();
    private final List<SessionAwareRuntimeComponent> mutableSessionComponents = new ArrayList<>();
    private final LinkedHashSet<String> initializationQuarantines = new LinkedHashSet<>();
    private volatile TickSlot[] tickSchedule = NO_TICK_SLOTS;
    private volatile SessionAwareRuntimeComponent[] sessionSchedule = NO_SESSION_COMPONENTS;
    private boolean initialized;

    private FeatureManager() {}

    public static FeatureManager getInstance() { return INSTANCE; }

    public synchronized void init() {
        if (initialized) return;

        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)) {
            registerComponent(new LavaHighlightFeature());
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)) {
            registerComponent(new AncientDebrisAnalyzerFeature());
        }
        if (hasAvailableWorksiteVisibilityFeature()) {
            registerComponent(new WorksiteVisibilityEngine());
        }

        for (RuntimeComponent component : components.values()) initializeComponent(component);

        tickSchedule = mutableTickSlots.toArray(TickSlot[]::new);
        sessionSchedule = mutableSessionComponents.toArray(SessionAwareRuntimeComponent[]::new);

        if (tickSchedule.length != 0) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                for (TickSlot slot : tickSchedule) slot.runForTick(client);
            });
        }
        initialized = true;
    }

    public synchronized void registerComponent(RuntimeComponent component) {
        requireMutableRegistration();
        Objects.requireNonNull(component, "component");
        String id = requireId(component.getId());
        if (components.putIfAbsent(id, component) != null) {
            throw new IllegalStateException("Duplicate component id: " + id);
        }
        registerSchedules(component);
    }

    public void resetSessionState(Minecraft client) {
        for (SessionAwareRuntimeComponent component : sessionSchedule) {
            TickSlot tickSlot = tickSlotFor(component);
            if (tickSlot != null && tickSlot.isQuarantined()) continue;
            try {
                component.resetSession(client);
            } catch (RuntimeException | LinkageError failure) {
                if (tickSlot != null) {
                    tickSlot.quarantineForLifecycleFailure(client, failure);
                } else {
                    ChiseTweaksClient.LOGGER.error(
                            "Client session reset skipped after {}",
                            failure.getClass().getSimpleName());
                }
            }
        }
    }

    public synchronized List<String> diagnosticQuarantinedComponentIds() {
        LinkedHashSet<String> result = new LinkedHashSet<>(initializationQuarantines);
        for (TickSlot slot : tickSchedule) {
            if (slot.isQuarantined()) result.add(slot.componentId());
        }
        return List.copyOf(result);
    }

    private TickSlot tickSlotFor(SessionAwareRuntimeComponent component) {
        for (TickSlot slot : tickSchedule) {
            if (slot.component == (Object) component) return slot;
        }
        return null;
    }

    private static boolean hasAvailableWorksiteVisibilityFeature() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            if (definition.isWorksiteVisibilityMode()
                    && FeatureAvailabilityPolicy.isAvailable(definition)) {
                return true;
            }
        }
        return false;
    }

    private void registerSchedules(RuntimeComponent component) {
        if (component instanceof TickingRuntimeComponent ticking) {
            mutableTickSlots.add(new TickSlot(ticking));
        }
        if (component instanceof SessionAwareRuntimeComponent sessionAware) {
            mutableSessionComponents.add(sessionAware);
        }
    }

    private void initializeComponent(RuntimeComponent component) {
        try {
            component.init();
        } catch (RuntimeException | LinkageError failure) {
            removeFromSchedules(component);
            initializationQuarantines.add(component.getId());
            if (component instanceof TickingRuntimeComponent ticking) {
                notifyInitializationQuarantine(component.getId(), ticking);
            }
            ChiseTweaksClient.LOGGER.error(
                    "Runtime component '{}' was quarantined during initialization after {}",
                    component.getId(),
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.COMPONENT_INIT_QUARANTINE,
                    Minecraft.getInstance(),
                    RuntimeDiagnosticDetail.of("componentId", component.getId()),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
        }
    }

    private void removeFromSchedules(RuntimeComponent component) {
        mutableTickSlots.removeIf(slot -> slot.component == component);
        mutableSessionComponents.removeIf(sessionAware -> sessionAware == (Object) component);
    }

    private static void notifyInitializationQuarantine(String id, TickingRuntimeComponent component) {
        try {
            component.onQuarantined(Minecraft.getInstance());
        } catch (RuntimeException | LinkageError cleanupFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Runtime component '{}' initialization cleanup failed after {}",
                    id,
                    cleanupFailure.getClass().getSimpleName());
        }
    }

    private void requireMutableRegistration() {
        if (initialized) throw new IllegalStateException("Components cannot be registered after initialization");
    }

    private static String requireId(String id) {
        String value = Objects.requireNonNull(id, "component id");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("component id must not be blank");
        if (!value.equals(normalized)) {
            throw new IllegalArgumentException("component id must not contain surrounding whitespace");
        }
        return value;
    }

    static final class TickSlot {
        private final TickingRuntimeComponent component;
        private int recoverableFailures;
        private volatile boolean quarantined;

        TickSlot(TickingRuntimeComponent component) {
            this.component = component;
        }

        void runForTick(Minecraft client) {
            if (quarantined) return;
            try {
                component.tick(client);
            } catch (RuntimeException | LinkageError failure) {
                recoverableFailures++;
                if (!FailureIsolationPolicy.shouldQuarantine(recoverableFailures)) return;
                quarantine(client, failure, "tick");
            }
        }

        void quarantineForLifecycleFailure(Minecraft client, Throwable failure) {
            quarantine(client, failure, "session-reset");
        }

        private void quarantine(Minecraft client, Throwable failure, String stage) {
            if (quarantined) return;
            quarantined = true;
            quarantineComponent(client);
            ChiseTweaksClient.LOGGER.error(
                    "Runtime component '{}' was quarantined during {} after {}",
                    component.getId(),
                    stage,
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.COMPONENT_QUARANTINE,
                    client,
                    RuntimeDiagnosticDetail.of("componentId", component.getId()),
                    RuntimeDiagnosticDetail.of("stage", stage),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
        }

        private void quarantineComponent(Minecraft client) {
            try {
                component.onQuarantined(client);
            } catch (RuntimeException | LinkageError cleanupFailure) {
                ChiseTweaksClient.LOGGER.warn(
                        "Runtime component '{}' cleanup failed after {}",
                        component.getId(),
                        cleanupFailure.getClass().getSimpleName());
            }
        }

        boolean isQuarantined() {
            return quarantined;
        }

        String componentId() {
            return component.getId();
        }
    }
}
