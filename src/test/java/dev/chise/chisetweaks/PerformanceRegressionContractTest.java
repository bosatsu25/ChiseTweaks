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
        String candidate = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanCandidate.java");
        String scanBody = between(scanner, "List<WorksiteVisibleTarget> scan(", "private void collectCandidate(");
        String collectBody = between(scanner, "private void collectCandidate(", "private WorksiteVisibleTarget materializeVisibleTarget(");

        assertTrue(scanner.contains("candidateBuffer"));
        assertTrue(scanner.contains("orderedBuffer"));
        assertTrue(scanner.contains("visibleBuffer"));
        assertTrue(scanner.contains("candidatePool"));
        assertTrue(scanner.contains("loadedChunkBuffer"));
        assertTrue(scanner.contains("BlockPos.MutableBlockPos cursor"));
        assertTrue(scanner.contains("BlockPos.MutableBlockPos visibilityCursor"));
        assertEquals(1, occurrences(scanner, ".hasChunk("));
        assertFalse(scanBody.contains("new PriorityQueue"));
        assertFalse(scanBody.contains("new ArrayList"));
        assertFalse(scanBody.contains("new BlockPos.MutableBlockPos"));
        assertFalse(collectBody.contains("new WorksiteScanCandidate"));
        assertTrue(candidate.contains("void assign("));
        assertTrue(scanBody.indexOf("hasChunk(chunkX, chunkZ)")
                < scanBody.indexOf("for (int z = minZ; z <= maxZ; z++)"));
    }

    @Test
    void scannerHardCapsLineOfSightRaycastsAndTheirShortLivedAllocations() throws IOException {
        String scanner = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");
        String budget = read("src/main/java/dev/chise/chisetweaks/core/performance/WorksiteVisibilityBudgetPolicy.java");
        String materialize = between(scanner,
                "private WorksiteVisibleTarget materializeVisibleTarget(",
                "/**\n     * Samples the actual occupied regions");
        String lineOfSight = between(scanner, "private boolean lineOfSight(", "private static double[][] samplesFor(");

        assertTrue(budget.contains("MAX_LINE_OF_SIGHT_RAYS_PER_SCAN = 192"));
        assertTrue(scanner.contains("remainingLineOfSightRays = WorksiteVisibilityBudgetPolicy.MAX_LINE_OF_SIGHT_RAYS_PER_SCAN"));
        assertTrue(scanner.contains("remainingLineOfSightRays <= 0"));
        assertEquals(1, occurrences(lineOfSight, "remainingLineOfSightRays--"));
        assertTrue(materialize.contains("visibilityCursor.set"));
        assertTrue(materialize.indexOf("lineOfSight(") < materialize.indexOf("new BlockPos("));
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
        String rowView = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingRowView.java");

        assertTrue(screen.contains("filteredRows"));
        assertTrue(screen.contains("rebuildFilteredRows()"));
        assertTrue(screen.contains("refreshDescriptionCache()"));
        assertTrue(screen.contains("row.renderedDescription"));
        assertTrue(screen.contains("row.searchableText.contains(query)"));
        assertTrue(rowView.contains("final String searchableText;"));
        assertTrue(rowView.contains("this.searchableText = normalizeSearchText(definition)"));
        assertFalse(screen.contains("visibleRows()"));
        assertFalse(screen.contains("List.copyOf(rows)"));

        String renderBody = between(screen, "public void extractRenderState(", "private void renderScrollbar(");
        String filterBody = between(screen, "private void rebuildFilteredRows()", "private void refreshDescriptionCache()");
        assertFalse(renderBody.contains("ellipsize("));
        assertFalse(renderBody.contains("toLowerCase("));
        assertFalse(renderBody.contains("new ArrayList"));
        assertFalse(filterBody.contains("row.definition.name() +"));
        assertFalse(filterBody.contains("row.definition.description())\n                    .toLowerCase"));
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
    void removedSoloUiDoesNotLeaveTransientTargetStateInTheConfigPath() throws IOException {
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        String saveBody = between(targets, "private static void save(Entry entry)", "private static Entry entry(");

        assertFalse(targets.contains("soloOreSelection"));
        assertFalse(targets.contains("toggleSoloOreSelection"));
        assertFalse(targets.contains("isSoloOreSelectionEnabled"));
        assertFalse(targets.contains("resetTransientControls"));
        assertFalse(saveBody.contains("withOnlyOreHighlightTarget"));
        assertEquals(1, occurrences(saveBody, "config.save()"));
    }

    @Test
    void uiAndRenderingResponsibilitiesRemainSplitIntoBoundedFiles() throws IOException {
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java", 520);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java", 390);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingRowView.java", 80);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java", 220);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLinePrimitives.java", 330);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java", 420);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java", 270);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java", 320);
        assertLineCountBelow("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanCandidate.java", 100);
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
