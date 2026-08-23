package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RepositoryRuntimeReviewContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeLifecycleUsesOneSlotAndRuntimeComponentOwnsCleanupContract() throws IOException {
        String feature = source("src/main/java/dev/chise/chisetweaks/feature/Feature.java");
        String manager = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String runtime = source("src/main/java/dev/chise/chisetweaks/runtime/RuntimeComponent.java");
        String ticking = source("src/main/java/dev/chise/chisetweaks/runtime/TickingRuntimeComponent.java");
        String worksite = source("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");

        assertTrue(feature.contains("public interface Feature extends RuntimeComponent"));
        assertTrue(manager.contains("Map<String, ComponentSlot> componentSlots"));
        assertTrue(manager.contains("for (ComponentSlot slot : sessionSchedule) slot.resetSession(client);"));
        assertTrue(manager.contains("component.onQuarantined(client);"));
        assertTrue(manager.contains("removeFromSchedules(slot);"));
        assertTrue(runtime.contains("default void onQuarantined(Minecraft client)"));
        assertFalse(ticking.contains("onQuarantined"));
        assertTrue(worksite.contains("public void onQuarantined(Minecraft client)"));
        assertFalse(manager.contains("tickSlotFor("));
        assertFalse(manager.contains("initializationQuarantines"));
        assertFalse(manager.contains("Map<String, RuntimeComponent>"));
        assertFalse(manager.contains("if (!active && !wasActive) return;"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/SessionAwareFeature.java")));
    }

    @Test
    void oreRefreshIsAnIndependentListenerInsteadOfOverwritingExclusiveMode() throws IOException {
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        assertTrue(bindings.contains("MATERIAL_HIGHLIGHTS.addValueChangeListener"));
        assertFalse(bindings.contains("MATERIAL_HIGHLIGHTS.setValueChangeCallback"));
    }

    @Test
    void persistenceFailuresRemainVisibleAndOnlyFailedDomainsStayDirty() throws IOException {
        String featureConfig = source("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");
        String localConfig = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureConfig.java");
        String coordinator = source("src/main/java/dev/chise/chisetweaks/config/SettingPersistenceCoordinator.java");
        String screen = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        assertTrue(featureConfig.contains("public static boolean saveToFile()"));
        assertTrue(localConfig.contains("public synchronized boolean save()"));
        assertTrue(coordinator.contains("catch (RuntimeException | LinkageError failure)"));
        assertTrue(coordinator.contains("return new SaveResult(failed);"));
        assertTrue(screen.contains("dirtyDomains.retainAll(result.failedDomains())"));
        assertTrue(screen.contains("if (!result.successful())"));
        assertTrue(screen.contains("screen.chisetweaks.settings.save_failed"));
        assertTrue(screen.contains("if (!applyChanges()) return;"));
    }

    @Test
    void qualityAndRepositoryAuditsCoverRuntimeBoundaryAndTrackedResidue() throws IOException {
        String build = source("build.gradle");
        String audit = source("scripts/repository_audit.py");
        assertTrue(build.contains("dev.chise.chisetweaks.runtime.FeatureManager$ComponentSlot*"));
        assertTrue(audit.contains("git\", \"ls-files\", \"-z"));
        assertTrue(audit.contains("FORBIDDEN_TRACKED_DIRECTORY_NAMES"));
    }

    @Test
    void removedPoliciesLegacyAssetsAndMisleadingWrappersCannotReturn() {
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/policy/BuilderEntityVisibilityPolicy.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/policy/ModVersionPolicy.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackFeature.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/models/block/visual/diamond_ore.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/models/block/visual/deepslate_diamond_ore.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/block/visual/diamond_ore_chise.png.mcmeta")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/block/visual/deepslate_diamond_ore_chise.png.mcmeta")));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
