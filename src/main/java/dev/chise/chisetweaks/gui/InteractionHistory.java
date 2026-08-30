package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.runtime.RuntimeComponent;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Memory-only, bounded interaction history. It never records chat, sign/book text or container contents. */
public final class InteractionHistory implements RuntimeComponent, SessionAwareRuntimeComponent {
    public static final int MAX_ENTRIES = 64;
    private static volatile InteractionHistory active;

    private final ArrayDeque<Entry> entries = new ArrayDeque<>(MAX_ENTRIES);
    private long sequence;

    @Override
    public String getId() {
        return "interaction-history";
    }

    @Override
    public void init() {
        active = this;
    }

    public static List<Entry> snapshot() {
        InteractionHistory current = active;
        if (current == null) return List.of();
        synchronized (current.entries) {
            return List.copyOf(current.entries);
        }
    }

    public static void recordBreak(BlockPos pos, BlockState state) {
        append(Type.BREAK, blockId(state), pos, "Block break");
    }

    public static void recordUseBlock(BlockPos pos, BlockState state) {
        append(Type.USE_BLOCK, blockId(state), pos, "Use block");
    }

    public static void recordUseItem(ItemStack stack) {
        String id = stack == null || stack.isEmpty()
                ? ""
                : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        append(Type.USE_ITEM, id, null, "Use item");
    }

    public static void recordAttackEntity(Entity entity) {
        String id = entity == null ? "" : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        append(Type.ATTACK_ENTITY, id, entity == null ? null : entity.blockPosition(), "Attack entity");
    }

    public static void recordInteractEntity(Entity entity) {
        String id = entity == null ? "" : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        append(Type.INTERACT_ENTITY, id, entity == null ? null : entity.blockPosition(), "Interact entity");
    }

    public static void recordPlacement(BlockPos pos, BlockState predicted, BlockState actual) {
        String schematic = PlacementInspector.schematicResultLabelAt(pos);
        String detail = "Placed";
        if (predicted != null && actual != null) {
            detail += predicted.equals(actual) ? " / MATCH" : " / ADJUSTED";
        }
        if (!schematic.isEmpty()) detail += " / Schematic " + schematic;
        append(Type.PLACEMENT, blockId(actual), pos, detail);
    }

    private static void append(Type type, String targetId, BlockPos pos, String detail) {
        if (!LocalFeatureConfig.getInstance().interactionHistoryEnabled) return;
        InteractionHistory current = active;
        if (current == null) return;
        Minecraft client = Minecraft.getInstance();
        long tick = client.level == null ? -1L : client.level.getGameTime();
        Entry entry = new Entry(
                ++current.sequence,
                tick,
                type,
                sanitize(targetId),
                pos == null ? null : pos.immutable(),
                sanitize(detail));
        synchronized (current.entries) {
            current.entries.addFirst(entry);
            while (current.entries.size() > MAX_ENTRIES) current.entries.removeLast();
        }
    }

    private static String blockId(BlockState state) {
        if (state == null) return "";
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    public static void clearHistory() {
        InteractionHistory current = active;
        if (current == null) return;
        synchronized (current.entries) {
            current.entries.clear();
        }
    }

    @Override
    public void resetSession(Minecraft client) {
        clearHistory();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        clearHistory();
        if (active == this) active = null;
    }

    public enum Type {
        PLACEMENT,
        BREAK,
        USE_BLOCK,
        USE_ITEM,
        ATTACK_ENTITY,
        INTERACT_ENTITY
    }

    public record Entry(
            long sequence,
            long gameTick,
            Type type,
            String targetId,
            BlockPos position,
            String detail) {

        public String summary() {
            StringBuilder value = new StringBuilder(type.name().replace('_', ' '));
            if (!targetId.isEmpty()) value.append("  ").append(targetId);
            if (position != null) {
                value.append(" @ ")
                        .append(position.getX()).append(' ')
                        .append(position.getY()).append(' ')
                        .append(position.getZ());
            }
            if (!detail.isEmpty()) value.append("\n").append(detail);
            return value.toString();
        }
    }
}
