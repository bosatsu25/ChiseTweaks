package dev.chise.chisetweaks.core.vision;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HandheldSizePolicyTest {
    @Test
    void categoriesAreSemanticAndDoNotDependOnModelJsonParents() {
        assertEquals(HandheldSizePolicy.Category.NONE, HandheldSizePolicy.classify(ItemStack.EMPTY));
        assertEquals(HandheldSizePolicy.Category.BLOCK, HandheldSizePolicy.classify(new ItemStack(Items.STONE)));
        assertEquals(HandheldSizePolicy.Category.TOOL, HandheldSizePolicy.classify(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertEquals(HandheldSizePolicy.Category.TOOL, HandheldSizePolicy.classify(new ItemStack(Items.BOW)));
        assertEquals(HandheldSizePolicy.Category.ITEM, HandheldSizePolicy.classify(new ItemStack(Items.STICK)));
        assertEquals(HandheldSizePolicy.Category.SHIELD, HandheldSizePolicy.classify(new ItemStack(Items.SHIELD)));
    }

    @Test
    void defaultsUseRelativeVanillaScaleRatiosAndKeepShieldNearVanilla() {
        assertEquals(0.70F, scale(Items.STONE), 0.0001F);
        assertEquals(0.75F, scale(Items.DIAMOND_PICKAXE), 0.0001F);
        assertEquals(0.60F, scale(Items.STICK), 0.0001F);
        assertEquals(0.95F, scale(Items.SHIELD), 0.0001F);
    }

    @Test
    void disabledOrNonFirstPersonRenderingRemainsVanilla() {
        ItemStack stack = new ItemStack(Items.STICK);
        assertEquals(1.0F, HandheldSizePolicy.scaleFactor(
                false,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                stack,
                70, 60, 75), 0.0001F);
        assertEquals(1.0F, HandheldSizePolicy.scaleFactor(
                true,
                ItemDisplayContext.GUI,
                stack,
                70, 60, 75), 0.0001F);
    }

    @Test
    void userScaleValuesAreBoundedBeforeRendering() {
        assertEquals(HandheldSizePolicy.MIN_SCALE_PERCENT, HandheldSizePolicy.clampScalePercent(-1));
        assertEquals(HandheldSizePolicy.MAX_SCALE_PERCENT, HandheldSizePolicy.clampScalePercent(500));
        assertEquals(73, HandheldSizePolicy.clampScalePercent(73));
    }

    private static float scale(net.minecraft.world.item.Item item) {
        return HandheldSizePolicy.scaleFactor(
                true,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                new ItemStack(item),
                HandheldSizePolicy.DEFAULT_BLOCK_SCALE_PERCENT,
                HandheldSizePolicy.DEFAULT_ITEM_SCALE_PERCENT,
                HandheldSizePolicy.DEFAULT_TOOL_SCALE_PERCENT);
    }
}
