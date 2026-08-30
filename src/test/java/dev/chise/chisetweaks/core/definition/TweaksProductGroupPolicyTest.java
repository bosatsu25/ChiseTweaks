package dev.chise.chisetweaks.core.definition;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TweaksProductGroupPolicyTest {
    @Test
    void everyRuntimeFeatureBelongsToExactlyOneTweaksProductGroup() {
        EnumMap<TweaksProductGroup, Integer> counts = new EnumMap<>(TweaksProductGroup.class);
        for (FeatureDefinition feature : FeatureDefinition.VALUES) {
            counts.merge(TweaksProductGroupPolicy.groupOf(feature), 1, Integer::sum);
        }

        assertEquals(4, counts.get(TweaksProductGroup.VISUAL_TWEAKS));
        assertEquals(6, counts.get(TweaksProductGroup.BUILDER_HIGHLIGHTS));
        assertEquals(4, counts.get(TweaksProductGroup.TECHNICAL_VISUALIZATION));
        assertEquals(2, counts.get(TweaksProductGroup.SCENE_FILTER));
    }

    @Test
    void representativeFeaturesStayInTheirProductFamilies() {
        assertEquals(TweaksProductGroup.BUILDER_HIGHLIGHTS,
                TweaksProductGroupPolicy.groupOf(FeatureDefinition.LAVA_HIGHLIGHT));
        assertEquals(TweaksProductGroup.BUILDER_HIGHLIGHTS,
                TweaksProductGroupPolicy.groupOf(FeatureDefinition.HIDDEN_SURFACE_TRACE));
        assertEquals(TweaksProductGroup.TECHNICAL_VISUALIZATION,
                TweaksProductGroupPolicy.groupOf(FeatureDefinition.VILLAGER_ANALYZER));
        assertEquals(TweaksProductGroup.VISUAL_TWEAKS,
                TweaksProductGroupPolicy.groupOf(FeatureDefinition.HANDHELD_SIZE));
        assertEquals(TweaksProductGroup.SCENE_FILTER,
                TweaksProductGroupPolicy.groupOf(FeatureDefinition.BUILDER_FOCUS_BLOCKS));
    }

    @Test
    void nullFeatureIsRejected() {
        assertThrows(NullPointerException.class, () -> TweaksProductGroupPolicy.groupOf(null));
    }
}
