package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import dev.chise.chisetweaks.feature.rendering.HiddenBlockAnalyzerFeature;
import dev.chise.chisetweaks.feature.rendering.InfrastructureRangeFeature;
import dev.chise.chisetweaks.feature.rendering.LavaHighlightFeature;
import dev.chise.chisetweaks.feature.rendering.VillagerAnalyzerFeature;
import dev.chise.chisetweaks.feature.rendering.worksite.WorksiteVisibilityEngine;
import dev.chise.chisetweaks.gui.InteractionHistory;
import dev.chise.chisetweaks.gui.PatternConsistencyInspector;
import dev.chise.chisetweaks.gui.PlacementComparisonTracker;
import dev.chise.chisetweaks.gui.SchematicPlacementInspector;
import dev.chise.chisetweaks.integration.compat.WorldBorderFixComponent;
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
    private static final ComponentSlot[] NO_SLOTS = new ComponentSlot[0];

    /** component identity・capability・quarantine状態を1 slotに集約する。 */
    private final Map<String, ComponentSlot> componentSlots = new LinkedHashMap<>();
    private final List<ComponentSlot> mutableTickSlots = new ArrayList<>();
    private final List<ComponentSlot> mutableSessionSlots = new ArrayList<>();
    private volatile ComponentSlot[] tickSchedule = NO_SLOTS;
    private volatile ComponentSlot[] sessionSchedule = NO_SLOTS;
    private boolean initialized;

    private FeatureManager() {}

    public static FeatureManager getInstance() { return INSTANCE; }

    public synchronized void init() {
        if (initialized) return;

        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)) {
            registerComponent(new LavaHighlightFeature());
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.VILLAGER_ANALYZER)) {
            registerComponent(new VillagerAnalyzerFeature());
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.HIDDEN_SURFACE_TRACE)) {
            registerComponent(new HiddenBlockAnalyzerFeature());
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BEACON_RANGE)) {
            registerComponent(new InfrastructureRangeFeature(InfrastructureRangeFeature.Mode.BEACON));
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LIGHTNING_ROD_RANGE)) {
            registerComponent(new InfrastructureRangeFeature(InfrastructureRangeFeature.Mode.LIGHTNING_ROD));
        }
        if (hasAvailableWorksiteVisibilityFeature()) {
            registerComponent(new WorksiteVisibilityEngine());
        }
        registerComponent(new PlacementComparisonTracker());
        registerComponent(new PatternConsistencyInspector());
        registerComponent(new InteractionHistory());
        registerComponent(new SchematicPlacementInspector());
        registerComponent(new WorldBorderFixComponent());

        for (ComponentSlot slot : componentSlots.values()) initializeComponent(slot);

        tickSchedule = mutableTickSlots.toArray(ComponentSlot[]::new);
        sessionSchedule = mutableSessionSlots.toArray(ComponentSlot[]::new);

        if (tickSchedule.length != 0) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                for (ComponentSlot slot : tickSchedule) slot.runForTick(client);
            });
        }
        initialized = true;
    }

    public synchronized void registerComponent(RuntimeComponent component) {
        requireMutableRegistration();
        RuntimeComponent checked = Objects.requireNonNull(component, "component");
        String id = requireId(checked.getId());
        ComponentSlot slot = new ComponentSlot(checked);
        if (componentSlots.putIfAbsent(id, slot) != null) {
            throw new IllegalStateException("Duplicate component id: " + id);
        }
        registerSchedules(slot);
    }

    public void resetSessionState(Minecraft client) {
        for (ComponentSlot slot : sessionSchedule) slot.resetSession(client);
    }

    public synchronized List<String> diagnosticQuarantinedComponentIds() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (ComponentSlot slot : componentSlots.values()) {
            if (slot.isQuarantined()) result.add(slot.componentId());
        }
        return List.copyOf(result);
    }

    private static boolean hasAvailableWorksiteVisibilityFeature() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            if (definition != FeatureDefinition.HIDDEN_SURFACE_TRACE
                    && definition.isWorksiteVisibilityMode()
                    && FeatureAvailabilityPolicy.isAvailable(definition)) {
                return true;
            }
        }
        return false;
    }

    private void registerSchedules(ComponentSlot slot) {
        if (slot.isTicking()) mutableTickSlots.add(slot);
        if (slot.isSessionAware()) mutableSessionSlots.add(slot);
    }

    private void initializeComponent(ComponentSlot slot) {
        try {
            slot.initialize();
        } catch (RuntimeException | LinkageError failure) {
            removeFromSchedules(slot);
            slot.quarantineDuringInitialization(Minecraft.getInstance(), failure);
        }
    }

    private void removeFromSchedules(ComponentSlot slot) {
        mutableTickSlots.remove(slot);
        mutableSessionSlots.remove(slot);
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

    static final class ComponentSlot {
        private final RuntimeComponent component;
        private final TickingRuntimeComponent ticking;
        private final SessionAwareRuntimeComponent sessionAware;
        private int recoverableTickFailures;
        private volatile boolean quarantined;

        ComponentSlot(RuntimeComponent component) {
            this.component = Objects.requireNonNull(component, "component");
            this.ticking = component instanceof TickingRuntimeComponent value ? value : null;
            this.sessionAware = component instanceof SessionAwareRuntimeComponent value ? value : null;
        }

        void initialize() {
            component.init();
        }

        void runForTick(Minecraft client) {
            if (quarantined || ticking == null) return;
            try {
                ticking.tick(client);
            } catch (RuntimeException | LinkageError failure) {
                recoverableTickFailures++;
                if (!FailureIsolationPolicy.shouldQuarantine(recoverableTickFailures)) return;
                quarantineRuntime(client, failure, "tick");
            }
        }

        void resetSession(Minecraft client) {
            if (quarantined || sessionAware == null) return;
            try {
                sessionAware.resetSession(client);
            } catch (RuntimeException | LinkageError failure) {
                quarantineRuntime(client, failure, "session-reset");
            }
        }

        void quarantineDuringInitialization(Minecraft client, Throwable failure) {
            if (!markQuarantined()) return;
            cleanup(client);
            ChiseTweaksClient.LOGGER.error(
                    "Runtime component '{}' was quarantined during initialization after {}",
                    component.getId(),
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.COMPONENT_INIT_QUARANTINE,
                    client,
                    RuntimeDiagnosticDetail.of("componentId", component.getId()),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
        }

        private void quarantineRuntime(Minecraft client, Throwable failure, String stage) {
            if (!markQuarantined()) return;
            cleanup(client);
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

        private boolean markQuarantined() {
            if (quarantined) return false;
            quarantined = true;
            return true;
        }

        private void cleanup(Minecraft client) {
            try {
                component.onQuarantined(client);
            } catch (RuntimeException | LinkageError cleanupFailure) {
                ChiseTweaksClient.LOGGER.warn(
                        "Runtime component '{}' cleanup failed after {}",
                        component.getId(),
                        cleanupFailure.getClass().getSimpleName());
            }
        }

        boolean isTicking() {
            return ticking != null;
        }

        boolean isSessionAware() {
            return sessionAware != null;
        }

        boolean isQuarantined() {
            return quarantined;
        }

        String componentId() {
            return component.getId();
        }
    }
}
