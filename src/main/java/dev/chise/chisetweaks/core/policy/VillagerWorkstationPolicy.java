package dev.chise.chisetweaks.core.policy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Vanilla profession -> workstation fallback mapping used only when client JOB_SITE memory is absent. */
public final class VillagerWorkstationPolicy {
    private VillagerWorkstationPolicy() {}

    public static Block workstation(String professionId) {
        String value = professionId == null ? "" : professionId.trim().toLowerCase(java.util.Locale.ROOT);
        int separator = value.indexOf(':');
        String path = separator >= 0 ? value.substring(separator + 1) : value;
        return switch (path) {
            case "armorer" -> Blocks.BLAST_FURNACE;
            case "butcher" -> Blocks.SMOKER;
            case "cartographer" -> Blocks.CARTOGRAPHY_TABLE;
            case "cleric" -> Blocks.BREWING_STAND;
            case "farmer" -> Blocks.COMPOSTER;
            case "fisherman" -> Blocks.BARREL;
            case "fletcher" -> Blocks.FLETCHING_TABLE;
            case "leatherworker" -> Blocks.CAULDRON;
            case "librarian" -> Blocks.LECTERN;
            case "mason" -> Blocks.STONECUTTER;
            case "shepherd" -> Blocks.LOOM;
            case "toolsmith" -> Blocks.SMITHING_TABLE;
            case "weaponsmith" -> Blocks.GRINDSTONE;
            default -> null;
        };
    }
}
