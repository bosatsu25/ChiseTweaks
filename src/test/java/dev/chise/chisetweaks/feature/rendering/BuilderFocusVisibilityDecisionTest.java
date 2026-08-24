package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.ChiseRuleMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuilderFocusVisibilityDecisionTest {
    private static final List<String> RENDER_CATEGORIES = List.of(
            "normal_block",
            "chest",
            "trapped_chest",
            "sign",
            "hanging_sign",
            "banner",
            "shulker_box",
            "beacon",
            "bed",
            "other_block_entity");

    @Test
    void filterOffIsAnAllowFastPathForEveryRenderCategory() {
        for (String category : RENDER_CATEGORIES) {
            assertFalse(
                    BuilderFocusVisibility.shouldHideByRule(
                            false, ChiseRuleMode.BLACKLIST, true, false),
                    category);
            assertFalse(
                    BuilderFocusVisibility.shouldHideByRule(
                            false, ChiseRuleMode.WHITELIST, false, false),
                    category);
        }
    }

    @Test
    void blacklistAndWhitelistApplyTheSameDecisionToNormalAndBlockEntityRendering() {
        for (String category : RENDER_CATEGORIES) {
            assertTrue(BuilderFocusVisibility.shouldHideByRule(
                    true, ChiseRuleMode.BLACKLIST, true, false), category);
            assertFalse(BuilderFocusVisibility.shouldHideByRule(
                    true, ChiseRuleMode.BLACKLIST, false, false), category);
            assertFalse(BuilderFocusVisibility.shouldHideByRule(
                    true, ChiseRuleMode.WHITELIST, false, true), category);
            assertTrue(BuilderFocusVisibility.shouldHideByRule(
                    true, ChiseRuleMode.WHITELIST, false, false), category);
        }
    }

    @Test
    void hideDecisionDoesNotDependOnBrightChestOrOtherVisualFeatures() {
        for (boolean brightChestEnabled : List.of(false, true)) {
            for (boolean allOtherVisualsEnabled : List.of(false, true)) {
                boolean hidden = BuilderFocusVisibility.shouldHideByRule(
                        true, ChiseRuleMode.BLACKLIST, true, false);
                assertTrue(hidden,
                        "brightChest=" + brightChestEnabled + ", allVisuals=" + allOtherVisualsEnabled);
            }
        }
    }

    @Test
    void noneAndInvalidModeFailOpen() {
        assertFalse(BuilderFocusVisibility.shouldHideByRule(
                true, ChiseRuleMode.NONE, true, false));
        assertFalse(BuilderFocusVisibility.shouldHideByRule(
                true, null, true, false));
    }
}
