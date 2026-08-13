package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/** Resolves a block state to one Chise-owned highlight style without scanning the world. */
public final class OreHighlightResolver {
    private static final ConcurrentHashMap<Block, Resolved> CACHE = new ConcurrentHashMap<>();
    private static final Resolved NONE = new Resolved(null, null);

    private OreHighlightResolver() {}

    public static @Nullable Resolved resolve(BlockState state) {
        if (state == null) return null;
        Resolved resolved = CACHE.computeIfAbsent(state.getBlock(), ignored -> resolveUncached(state));
        return resolved == NONE ? null : resolved;
    }

    public static boolean isCandidate(BlockState state) {
        return resolve(state) != null;
    }

    public static void invalidateCache() {
        CACHE.clear();
    }

    private static Resolved resolveUncached(BlockState state) {
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String blockId = id.toString();

        Target vanillaTarget = VanillaOreVisualCatalog.targetForBlockId(blockId);
        if (vanillaTarget != null) {
            OreHighlightStyle style = OreHighlightStyle.fromKey(
                    VanillaOreVisualCatalog.highlightKeyForBlockId(blockId));
            return style == null ? NONE : new Resolved(vanillaTarget, style);
        }
        if (block == Blocks.OBSIDIAN) {
            return new Resolved(Target.MATERIAL_OBSIDIAN, OreHighlightStyle.OBSIDIAN);
        }
        if (block == Blocks.CRYING_OBSIDIAN) {
            return new Resolved(Target.MATERIAL_CRYING_OBSIDIAN, OreHighlightStyle.CRYING_OBSIDIAN);
        }
        if ("minecraft".equals(id.getNamespace())) return NONE;

        OreHighlightStyle explicit = OreHighlightExternalRegistry.styleForBlockId(blockId);
        if (explicit != null) return new Resolved(null, explicit);

        OreHighlightStyle apiTag = OreHighlightExternalRegistry.styleForApiTag(state);
        if (apiTag != null) return new Resolved(null, apiTag);

        OreHighlightStyle conventional = conventionalStyle(state);
        if (conventional != null) return new Resolved(null, conventional);

        if (ModdedOreIdPolicy.looksLikeOre(blockId)) {
            return new Resolved(null, OreHighlightStyle.GENERIC);
        }
        return NONE;
    }

    private static @Nullable OreHighlightStyle conventionalStyle(BlockState state) {
        if (state.is(ConventionalBlockTags.COAL_ORES)) return OreHighlightStyle.COAL;
        if (state.is(ConventionalBlockTags.IRON_ORES)) return OreHighlightStyle.IRON;
        if (state.is(ConventionalBlockTags.COPPER_ORES)) return OreHighlightStyle.COPPER;
        if (state.is(ConventionalBlockTags.GOLD_ORES)) return OreHighlightStyle.GOLD;
        if (state.is(ConventionalBlockTags.LAPIS_ORES)) return OreHighlightStyle.LAPIS;
        if (state.is(ConventionalBlockTags.REDSTONE_ORES)) return OreHighlightStyle.REDSTONE;
        if (state.is(ConventionalBlockTags.DIAMOND_ORES)) return OreHighlightStyle.DIAMOND;
        if (state.is(ConventionalBlockTags.EMERALD_ORES)) return OreHighlightStyle.EMERALD;
        if (state.is(ConventionalBlockTags.QUARTZ_ORES)) return OreHighlightStyle.QUARTZ;
        if (state.is(ConventionalBlockTags.NETHERITE_SCRAP_ORES)) return OreHighlightStyle.ANCIENT_DEBRIS;
        if (state.is(ConventionalBlockTags.ORES)) return OreHighlightStyle.GENERIC;
        return null;
    }

    /** A null target means a modded ore that follows the master Ore Highlights switch. */
    public record Resolved(@Nullable Target target, @Nullable OreHighlightStyle style) {}
}
