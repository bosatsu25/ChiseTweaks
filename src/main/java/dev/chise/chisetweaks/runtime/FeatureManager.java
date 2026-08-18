package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.PreReleaseFeaturePolicy;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import dev.chise.chisetweaks.feature.Feature;
import dev.chise.chisetweaks.feature.SessionAwareFeature;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.feature.rendering.LavaHighlightFeature;
import dev.chise.chisetweaks.feature.rendering.worksite.WorksiteVisibilityEngine;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns retained user-facing features and shared runtime components. */
public final class FeatureManager {
    private static final FeatureManager INSTANCE = new FeatureManager();
    private static final TickSlot[] NO_TICK_SLOTS = new TickSlot[0];
    private static final SessionAwareRuntimeComponent[] NO_SESSION_COMPONENTS =
            new SessionAwareRuntimeComponent[0];

    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Map<String, Feature> readOnlyFeatures = Collections.unmodifiableMap(features);
    private final Map<String, RuntimeComponent> runtimeComponents = new LinkedHashMap<>();
    private final Map<String, RuntimeComponent> readOnlyRuntimeComponents =
            Collections.unmodifiableMap(runtimeComponents);
    private final List<TickSlot> mutableTickSlots = new ArrayList<>();
    private final List<SessionAwareRuntimeComponent> mutableSessionComponents = new ArrayList<>();
    private volatile TickSlot[] tickSchedule = NO_TICK_SLOTS;
    private volatile SessionAwareRuntimeComponent[] sessionSchedule = NO_SESSION_COMPONENTS;
    private boolean initialized;

    private FeatureManager() {}

    public static FeatureManager getInstance() { return INSTANCE; }

    public synchronized void init() {
        if (initialized) return;

        if (PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)) {
            registerFeature(new LavaHighlightFeature());
        }
        if (hasAvailableWorksiteVisibilityFeature()) {
            registerRuntimeComponent(new WorksiteVisibilityEngine());
        }

        for (Feature feature : features.values()) initFeature(feature);
        for (RuntimeComponent component : runtimeComponents.values()) initRuntimeComponent(component);

        tickSchedule = mutableTickSlots.toArray(TickSlot[]::new);
        sessionSchedule = mutableSessionComponents.toArray(SessionAwareRuntimeComponent[]::new);

        if (tickSchedule.length != 0) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                TickSlot[] schedule = tickSchedule;
                for (int index = 0; index < schedule.length; index++) {
                    schedule[index].runForTick(client);
                }
            });
        }
        initialized = true;
    }

    public synchronized void registerFeature(Feature feature) {
        requireMutableRegistration();
        Objects.requireNonNull(feature, "feature");
        String id = requireId(feature.getId(), "feature id");
        if (features.putIfAbsent(id, feature) != null) {
            throw new IllegalStateException("Duplicate feature id: " + id);
        }
        if (feature instanceof TickingFeature ticking) mutableTickSlots.add(new TickSlot(ticking));
        if (feature instanceof SessionAwareFeature sessionAware) mutableSessionComponents.add(sessionAware);
    }

    public synchronized void registerRuntimeComponent(RuntimeComponent component) {
        requireMutableRegistration();
        Objects.requireNonNull(component, "component");
        String id = requireId(component.getId(), "component id");
        if (runtimeComponents.putIfAbsent(id, component) != null) {
            throw new IllegalStateException("Duplicate runtime component id: " + id);
        }
        if (component instanceof TickingRuntimeComponent ticking) mutableTickSlots.add(new TickSlot(ticking));
        if (component instanceof SessionAwareRuntimeComponent sessionAware) {
            mutableSessionComponents.add(sessionAware);
        }
    }

    public Feature getFeature(String id) { return features.get(id); }
    public Map<String, Feature> getFeatures() { return readOnlyFeatures; }
    public RuntimeComponent getRuntimeComponent(String id) { return runtimeComponents.get(id); }
    public Map<String, RuntimeComponent> getRuntimeComponents() { return readOnlyRuntimeComponents; }
    public int getTickingComponentCount() {
        return tickSchedule.length == 0 ? mutableTickSlots.size() : tickSchedule.length;
    }
    public int getSessionAwareComponentCount() {
        return sessionSchedule.length == 0 ? mutableSessionComponents.size() : sessionSchedule.length;
    }

    public void resetSessionState(Minecraft client) {
        SessionAwareRuntimeComponent[] schedule = sessionSchedule;
        for (int index = 0; index < schedule.length; index++) {
            try {
                schedule[index].resetSession(client);
            } catch (RuntimeException | LinkageError failure) {
                ChiseTweaksClient.LOGGER.error(
                        "Client session reset skipped after {}",
                        failure.getClass().getSimpleName());
            }
        }
    }

    private static boolean hasAvailableWorksiteVisibilityFeature() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            if (definition.isWorksiteVisibilityMode()
                    && PreReleaseFeaturePolicy.isAvailable(definition)) {
                return true;
            }
        }
        return false;
    }

    private void initFeature(Feature feature) {
        try {
            feature.init();
        } catch (RuntimeException | LinkageError failure) {
            safeDisable(feature);
            quarantineTickSlot(feature.getId());
            ChiseTweaksClient.LOGGER.error(
                    "Feature '{}' was disabled during initialization after {}",
                    feature.getId(),
                    failure.getClass().getSimpleName());
        }
    }

    private void initRuntimeComponent(RuntimeComponent component) {
        try {
            component.init();
        } catch (RuntimeException | LinkageError failure) {
            quarantineTickSlot(component.getId());
            ChiseTweaksClient.LOGGER.error(
                    "Runtime component '{}' was quarantined during initialization after {}",
                    component.getId(),
                    failure.getClass().getSimpleName());
        }
    }

    private void quarantineTickSlot(String id) {
        for (TickSlot slot : mutableTickSlots) {
            if (slot.component.getId().equals(id)) slot.quarantined = true;
        }
    }

    private void requireMutableRegistration() {
        if (initialized) throw new IllegalStateException("Components cannot be registered after initialization");
    }

    private static String requireId(String id, String label) {
        String normalized = Objects.requireNonNull(id, label).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }

    private static void safeDisable(Feature feature) {
        try {
            feature.setEnabled(false);
        } catch (RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Feature '{}' could not be disabled after {}",
                    feature.getId(),
                    failure.getClass().getSimpleName());
        }
    }

    static final class TickSlot {
        private final TickingRuntimeComponent component;
        private int recoverableFailures;
        private boolean quarantined;

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
                quarantined = true;
                quarantineComponent(client);
                if (component instanceof Feature feature) safeDisable(feature);
                ChiseTweaksClient.LOGGER.error(
                        "Runtime component '{}' was quarantined after {}",
                        component.getId(),
                        failure.getClass().getSimpleName());
            }
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
    }
}
