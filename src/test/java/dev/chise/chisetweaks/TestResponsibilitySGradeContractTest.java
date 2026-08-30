package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** S7: proves that each quality risk has an owned verification layer. */
final class TestResponsibilitySGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path TESTS = ROOT.resolve("src/test/java/dev/chise/chisetweaks");
    private static final Path SELF = ROOT.resolve(
            "src/test/java/dev/chise/chisetweaks/TestResponsibilitySGradeContractTest.java");

    @Test
    void deterministicPolicyBehaviorHasJUnitAndMutationEvidence() throws IOException {
        assertExists("build.gradle");
        String build = Files.readString(ROOT.resolve("build.gradle"));
        assertTrue(build.contains("jacocoTestCoverageVerification"));
        assertTrue(build.contains("pitest {"));
        assertTrue(build.contains("mutationThreshold"));
        assertTrue(build.contains("testStrengthThreshold"));
        assertAnyOtherTestMentions("WorksiteVisibilityBudgetPolicy");
        assertAnyOtherTestMentions("VisualTargetSelectionPolicy");
    }

    @Test
    void runtimeFeatureWiringAndFaultIsolationHaveExecutableRegressionEvidence() {
        assertExists("src/test/java/dev/chise/chisetweaks/RuntimeFeatureUsabilityRegressionTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/runtime/FeatureManagerFaultInjectionSGradeTest.java");
    }

    @Test
    void performanceHasStructuralAndRealDeviceEvidenceLayers() throws IOException {
        assertAnyOtherTestMentions("PerformanceArchitectureContract");
        assertExists("scripts/performance_evidence_template.py");
        assertExists("scripts/prism_acceptance_audit.py");
        String build = Files.readString(ROOT.resolve("build.gradle"));
        assertTrue(build.contains("comparePerformanceEvidence"));
    }

    @Test
    void configAndSecurityHaveAdversarialTestOwnership() {
        assertExists("src/test/java/dev/chise/chisetweaks/core/security/ConfigAdversarialSGradeTest.java");
    }

    @Test
    void releaseRegressionHasArtifactParityAndProvenanceLayers() {
        for (String path : List.of(
                "scripts/artifact_audit.py",
                "scripts/functional_parity_audit.py",
                "scripts/ci_provenance.py",
                "scripts/release_residue_audit.py")) {
            assertExists(path);
        }
    }

    @Test
    void minecraftSemanticsHaveClientGameTestSourceSet() throws IOException {
        Path clientTests = ROOT.resolve("src/clientTest/java");
        assertTrue(Files.isDirectory(clientTests), "Client GameTest source set is missing");
        try (Stream<Path> files = Files.walk(clientTests)) {
            assertTrue(files.anyMatch(path -> path.toString().endsWith(".java")),
                    "Client GameTest must contain executable Java tests");
        }
    }

    private static void assertAnyOtherTestMentions(String marker) throws IOException {
        try (Stream<Path> files = Files.walk(TESTS)) {
            boolean found = false;
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (path.toAbsolutePath().normalize().equals(SELF)) continue;
                if (Files.readString(path).contains(marker)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, () -> "No independent JUnit evidence found for " + marker);
        }
    }

    private static void assertExists(String relativePath) {
        assertTrue(Files.isRegularFile(ROOT.resolve(relativePath)), () -> "Missing evidence: " + relativePath);
    }
}
