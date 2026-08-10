package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityConstructorCoverageTest {
    @Test
    void retainedPurePolicyUtilitiesRemainNonPublicAndConstructibleOnlyByReflection() throws Exception {
        for (Class<?> type : List.of(
                WorksiteVisibilityBudgetPolicy.class,
                BuilderEntityVisibilityPolicy.class,
                ConfigListPolicy.class,
                LavaVisionPalettePolicy.class,
                WorksiteVisibilitySelectionPolicy.class,
                VanillaOreVisualCatalog.class,
                VisualTargetGroupPolicy.class,
                VisualTargetSelectionPolicy.class)) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), type.getName());
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance(), type.getName());
        }
    }
}
