package dev.chise.chisetweaks.core.policy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Read-only schematic comparison. It never changes or cancels placement. */
public final class SchematicPlacementComparisonPolicy {
    public static final int NONE = 0;
    public static final int MATCH = 1;
    public static final int COMPATIBLE = 2;
    public static final int DIFFERENT = 3;

    private SchematicPlacementComparisonPolicy() {}

    public static int compare(
            BlockState expected,
            BlockState predicted,
            List<String> compatibilityMap) {
        if (expected == null || predicted == null || expected.isAir()) return NONE;
        if (expected.equals(predicted)) return MATCH;
        if (expected.getBlock() == predicted.getBlock()) return COMPATIBLE;

        String expectedId = String.valueOf(BuiltInRegistries.BLOCK.getKey(expected.getBlock()));
        String predictedId = String.valueOf(BuiltInRegistries.BLOCK.getKey(predicted.getBlock()));
        String replacement = LitematicaPickRedirectPolicy.replacementFor(expectedId, compatibilityMap);
        return predictedId.equals(replacement) ? COMPATIBLE : DIFFERENT;
    }

    public static String label(int result) {
        return switch (result) {
            case MATCH -> "MATCH";
            case COMPATIBLE -> "COMPATIBLE";
            case DIFFERENT -> "DIFFERENT";
            default -> "";
        };
    }
}
