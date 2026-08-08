package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityClassCoverageContractTest {
    private static final List<String> UTILITY_CLASSES = List.of(
            "dev.chise.chisetweaks.config.FeatureConfigDocumentPolicy",
            "dev.chise.chisetweaks.config.LocalFeatureConfigDocumentPolicy",
            "dev.chise.chisetweaks.core.performance.WorksiteCandidateRetentionPolicy",
            "dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy",
            "dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy",
            "dev.chise.chisetweaks.core.policy.ConfigListPolicy",
            "dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy",
            "dev.chise.chisetweaks.core.policy.ModVersionPolicy",
            "dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy",
            "dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy",
            "dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy",
            "dev.chise.chisetweaks.core.security.SecureConfigStorage",
            "dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy");

    @Test
    void deterministicKernelUtilityTypesExposeOnlyPrivateNoArgConstruction() throws Exception {
        for (String className : UTILITY_CLASSES) {
            Class<?> type = Class.forName(className);
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            assertEquals(1, constructors.length, className);

            Constructor<?> constructor = constructors[0];
            assertEquals(0, constructor.getParameterCount(), className);
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), className);

            // Exercise the otherwise unreachable constructor line so the strict 96% line gate
            // does not penalize utility-class boilerplate while still asserting it stays private.
            constructor.setAccessible(true);
            constructor.newInstance();
        }
    }
}
