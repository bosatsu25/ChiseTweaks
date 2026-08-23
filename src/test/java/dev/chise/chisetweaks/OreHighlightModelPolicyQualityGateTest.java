package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic runtime state quality gate for model-backed Ore Highlights. */
final class OreHighlightModelPolicyQualityGateTest {
    @Test
    void visualTargetGroupsRemainDisjointCompleteAndKeepOreCatalogInsideMaterialGroup() {
        int material = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.MATERIAL);
        int technical = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.TECHNICAL);
        int hidden = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.HIDDEN);

        assertEquals(0, material & technical);
        assertEquals(0, material & hidden);
        assertEquals(0, technical & hidden);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                material | technical | hidden);
        assertEquals(VanillaOreVisualCatalog.TARGET_MASK & material,
                VanillaOreVisualCatalog.TARGET_MASK);
    }

    @Test
    void runtimePolicyRequiresMasterAndExactMaterialTarget() {
        int diamond = Target.MATERIAL_DIAMOND_ORE.bitMask();
        int redstone = Target.MATERIAL_REDSTONE_ORE.bitMask();
        assertTrue(OreHighlightRuntimePolicy.shouldRender(true, diamond, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(false, diamond, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(true, redstone, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(true, diamond, Target.MATERIAL_REDSTONE_ORE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(true, diamond, Target.HIDDEN_BLUE_ICE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(true, diamond, Target.TECHNICAL_TRIPWIRE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(true, diamond, null));
    }

    @Test
    void runtimePolicyAcceptsEveryConfiguredMaterialFamilyAndRejectsNonMaterialFamilies() {
        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        for (Target target : Target.values()) {
            if (VisualTargetSelectionPolicy.isOreHighlightTarget(target)) {
                assertTrue(OreHighlightRuntimePolicy.shouldRender(true, all, target), target.name());
            } else {
                assertFalse(OreHighlightRuntimePolicy.shouldRender(true, all, target), target.name());
            }
        }
    }

    @Test
    void runtimePolicySanitizesUnknownMaskBitsThroughTargetSelection() {
        assertTrue(OreHighlightRuntimePolicy.shouldRender(
                true, Integer.MAX_VALUE, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(OreHighlightRuntimePolicy.shouldRender(
                true, Integer.MIN_VALUE, Target.MATERIAL_DIAMOND_ORE));
    }

    @Test
    void reducedMotionIsTheDefaultRuntimeStyle() {
        assertEquals(OreHighlightRuntimePolicy.Motion.STATIC,
                OreHighlightRuntimePolicy.motion(false));
        assertEquals(OreHighlightRuntimePolicy.Motion.ANIMATED,
                OreHighlightRuntimePolicy.motion(true));
    }
}
