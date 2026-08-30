package dev.chise.chisetweaks.core.vision;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Semantic first-person item scaling without resource-pack model replacement.
 *
 * <p>Chise applies bounded relative multipliers to Minecraft's existing first-person
 * rendering result. Models, textures, animation transforms and resource-pack ownership
 * remain unchanged.</p>
 */
public final class HandheldSizePolicy {
    public static final int MIN_SCALE_PERCENT = 40;
    public static final int MAX_SCALE_PERCENT = 100;
    public static final int DEFAULT_BLOCK_SCALE_PERCENT = 70;
    public static final int DEFAULT_ITEM_SCALE_PERCENT = 60;
    public static final int DEFAULT_TOOL_SCALE_PERCENT = 75;
    public static final int SHIELD_SCALE_PERCENT = 95;

    public enum Category {
        NONE,
        BLOCK,
        ITEM,
        TOOL,
        SHIELD
    }

    private HandheldSizePolicy() {}

    public static Category classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Category.NONE;
        if (stack.getItem() == Items.SHIELD) return Category.SHIELD;
        if (stack.getItem() instanceof BlockItem) return Category.BLOCK;
        if (stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON)
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof FishingRodItem) {
            return Category.TOOL;
        }
        return Category.ITEM;
    }

    public static int clampScalePercent(int value) {
        return Math.max(MIN_SCALE_PERCENT, Math.min(MAX_SCALE_PERCENT, value));
    }

    public static float scaleFactor(
            boolean enabled,
            ItemDisplayContext context,
            ItemStack stack,
            int blockScalePercent,
            int itemScalePercent,
            int toolScalePercent) {
        if (!enabled || !isFirstPerson(context)) return 1.0F;
        return switch (classify(stack)) {
            case NONE -> 1.0F;
            case BLOCK -> clampScalePercent(blockScalePercent) / 100.0F;
            case ITEM -> clampScalePercent(itemScalePercent) / 100.0F;
            case TOOL -> clampScalePercent(toolScalePercent) / 100.0F;
            case SHIELD -> SHIELD_SCALE_PERCENT / 100.0F;
        };
    }

    public static boolean isFirstPerson(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
}
