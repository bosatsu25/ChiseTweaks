package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceRegressionContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void boundedScannerProbesEachIntersectedChunkOnceAndReusesHotBuffers() throws IOException {
        String scanner = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");
        String scanBody = between(scanner, "List<WorksiteVisibleTarget> scan(", "private void collectCandidate(");

        assertTrue(scanner.contains("candidateBuffer"));
        assertTrue(scanner.contains("orderedBuffer"));
        assertTrue(scanner.contains("visibleBuffer"));
        assertTrue(scanner.contains("loadedChunkBuffer"));
        assertTrue(scanner.contains("BlockPos.MutableBlockPos cursor"));
        assertEquals(1, occurrences(scanner, ".hasChunk("));
        assertFalse(scanBody.contains("new PriorityQueue"));
        assertFalse(scanBody.contains("new ArrayList"));
        assertFalse(scanBody.contains("new BlockPos.MutableBlockPos"));
        assertTrue(scanBody.indexOf("hasChunk(chunkX, chunkZ)")
                < scanBody.indexOf("for (int z = minZ; z <= maxZ; z++)"));
    }

    @Test
    void worldOverlayEngineAvoidsPerFrameFeatureWalksAndIdleRescans() throws IOException {
        String engine = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");

        assertTrue(engine.contains("private volatile boolean active;"));
        assertTrue(engine.contains("return active;"));
        assertTrue(engine.contains("WorksiteScanThrottlePolicy.shouldScan"));
        assertTrue(engine.contains("movementObserved"));
        assertTrue(engine.contains("scanFingerprint"));
        assertTrue(engine.contains("config.worksiteVisibilityWorldOverlay"));
        assertFalse(between(engine, "public boolean isActive()", "@Override\n    public void tick")
                .contains("FeatureSwitches.VALUES"));
    }

    @Test
    void surfaceGeometryHotPathDoesNotAllocateTemporaryFloatArrays() throws IOException {
        String facade = read("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java");
        String primitives = read("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLinePrimitives.java");
        String placement = read("src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java");

        assertFalse(facade.contains("new float[]"));
        assertFalse(primitives.contains("new float[]"));
        assertFalse(placement.contains("new float[]"));
        assertTrue(facade.contains("SurfaceLinePrimitives"));
        assertTrue(facade.contains("PlacementGuideLineGeometry"));
    }

    @Test
    void modelTargetEditsAreDebouncedBeforeFullResourceReload() throws IOException {
        String coordinator = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualModelReloadCoordinator.java");
        String policy = read("src/main/java/dev/chise/chisetweaks/core/performance/VisualModelReloadThrottlePolicy.java");

        assertTrue(coordinator.contains("VisualModelReloadThrottlePolicy"));
        assertTrue(coordinator.contains("shouldRequestReload"));
        assertTrue(coordinator.contains("onReloadFailed"));
        assertTrue(coordinator.contains("onReloadSucceeded"));
        assertTrue(policy.contains("QUIET_TICKS = 4"));
        assertTrue(policy.contains("FAILURE_BACKOFF_TICKS = 100"));
        assertEquals(1, occurrences(coordinator, "reloadResourcePacks()"));
    }

    @Test
    void settingsScreenCachesFilteringAndEllipsisInsteadOfAllocatingEveryFrame() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(screen.contains("filteredRows"));
        assertTrue(screen.contains("rebuildFilteredRows()"));
        assertTrue(screen.contains("refreshDescriptionCache()"));
        assertTrue(screen.contains("row.renderedDescription"));
        assertFalse(screen.contains("visibleRows()"));
        assertFalse(screen.contains("List.copyOf(rows)"));

        String renderBody = between(screen, "public void extractRenderState(", "private void renderScrollbar(");
        assertFalse(renderBody.contains("ellipsize("));
        assertFalse(renderBody.contains("toLowerCase("));
        assertFalse(renderBody.contains("new ArrayList"));
    }

    @Test
    void bulkTargetChangesUseOneMaskMutationInsteadOfNIndividualConfigWrites() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String bulk = between(controller, "void toggleBulk(", "boolean resetSection(");

        assertTrue(bulk.contains("VisualTargetGroupPolicy.withAll"));
        assertEquals(1, occurrences(bulk, "config.save()"));
        assertFalse(bulk.contains("for ("));
        assertFalse(bulk.contains("setBooleanValue"));
    }

    @Test
    void uiAndRenderingResponsibilitiesRemainSplitIntoBoundedFiles() throws IOException {
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java", 520);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java", 390);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java", 220);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLinePrimitives.java", 330);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java", 420);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java", 260);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java", 320);
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, startToken);
        assertTrue(end > start, endToken);
        return source.substring(start, end);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertLineCountBelow(String relative, long maximumExclusive) throws IOException {
        long lines = read(relative).lines().count();
        assertTrue(lines < maximumExclusive, relative + " lines=" + lines + " limit=" + maximumExclusive);
    }
}
