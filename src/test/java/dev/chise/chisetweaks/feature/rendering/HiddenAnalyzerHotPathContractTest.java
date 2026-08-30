package dev.chise.chisetweaks.feature.rendering;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HiddenAnalyzerHotPathContractTest {
    @Test
    void scanLoopUsesCachedBlockTargetMaskInsteadOfRegistryStringClassification() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java"));

        int start = source.indexOf("private int scanLoadedTargets(");
        int end = source.indexOf("private boolean hasKnownSourceBoundary(", start);
        assertTrue(start >= 0 && end > start);
        String scan = source.substring(start, end);

        assertTrue(scan.contains("int targetMask = hiddenTargetMask(block)"));
        assertTrue(scan.contains("(local.visualTargetMask & targetMask) != 0"));
        assertFalse(scan.contains("BuiltInRegistries.BLOCK.getKey"));
        assertFalse(scan.contains("BlockInspectionPolicy.matches"));
        assertFalse(scan.contains("VisualTargetSelectionPolicy.matchesEnabled"));

        assertTrue(source.contains("java.util.IdentityHashMap"));
        assertTrue(source.contains("Integer cached = hiddenTargetMasks.get(block)"));
        assertTrue(source.contains("hiddenTargetMasks.put(block, targetMask)"));
    }
}
