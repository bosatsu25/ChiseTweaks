package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3: hidden analyzer classification must stay allocation-light in the bounded world-scan loop. */
final class ThroughWallAnalyzerHotPathSGradeTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void hiddenScanDoesNotConvertRegistryIdentifiersToStrings() throws Exception {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java"));

        assertTrue(source.contains("hiddenTargetEnabled(state.getBlock(), local.visualTargetMask)"));
        assertFalse(source.contains("BuiltInRegistries.BLOCK.getKey"));
        assertFalse(source.contains("Identifier id ="));
        assertFalse(source.contains("blockId = id == null ? \"\" : id.toString()"));
        assertFalse(source.contains("BlockInspectionPolicy.matches("));
        assertFalse(source.contains("VisualTargetSelectionPolicy.matchesEnabled("));
    }

    @Test
    void directBlockClassificationPreservesIndependentTargetBits() throws Exception {
        assertTarget(Blocks.BLUE_ICE, VisualTargetSelectionPolicy.Target.HIDDEN_BLUE_ICE);
        assertTarget(Blocks.POWDER_SNOW, VisualTargetSelectionPolicy.Target.HIDDEN_POWDER_SNOW);
        assertTarget(Blocks.SCULK_CATALYST, VisualTargetSelectionPolicy.Target.HIDDEN_SCULK_CATALYST);
        assertTarget(Blocks.DEAD_TUBE_CORAL_BLOCK, VisualTargetSelectionPolicy.Target.HIDDEN_DEAD_CORAL);
        assertTarget(Blocks.DEAD_BRAIN_CORAL, VisualTargetSelectionPolicy.Target.HIDDEN_DEAD_CORAL);
        assertTarget(Blocks.DEAD_BUBBLE_CORAL_FAN, VisualTargetSelectionPolicy.Target.HIDDEN_DEAD_CORAL);
        assertTarget(Blocks.DEAD_FIRE_CORAL_WALL_FAN, VisualTargetSelectionPolicy.Target.HIDDEN_DEAD_CORAL);
    }

    @Test
    void unrelatedBlocksNeverMatchHiddenAnalyzer() throws Exception {
        Method method = classifier();
        int allHidden = VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK;
        assertFalse((boolean) method.invoke(null, Blocks.STONE, allHidden));
        assertFalse((boolean) method.invoke(null, Blocks.DIAMOND_ORE, allHidden));
        assertFalse((boolean) method.invoke(null, Blocks.LAVA, allHidden));
    }

    private static void assertTarget(Block block, VisualTargetSelectionPolicy.Target target) throws Exception {
        Method method = classifier();
        assertTrue((boolean) method.invoke(null, block, target.bitMask()));
        assertFalse((boolean) method.invoke(null, block, 0));
        int otherHiddenBits = VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK & ~target.bitMask();
        assertFalse((boolean) method.invoke(null, block, otherHiddenBits));
    }

    private static Method classifier() throws NoSuchMethodException {
        Method method = ThroughWallAnalyzerFeature.class.getDeclaredMethod(
                "hiddenTargetEnabled", Block.class, int.class);
        method.setAccessible(true);
        return method;
    }
}
