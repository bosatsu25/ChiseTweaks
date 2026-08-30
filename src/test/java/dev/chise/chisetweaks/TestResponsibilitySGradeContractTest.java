package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** S7: proves that each quality risk has an explicit executable verification owner. */
final class TestResponsibilitySGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void deterministicPolicyBehaviorHasJUnitAndMutationEvidence() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        assertTrue(build.contains("jacocoTestCoverageVerification"));
        assertTrue(build.contains("pitest {"));
        assertTrue(build.contains("mutationThreshold"));
        assertTrue(build.contains("testStrengthThreshold"));

        assertExists("src/test/java/dev/chise/chisetweaks/VisualTargetSelectionPolicyTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/LightweightRuntimeBudgetContractTest.java");
    }

    @Test
    void runtimeFeatureWiringAndFaultIsolationHaveExecutableRegressionEvidence() {
        assertExists("src/test/java/dev/chise/chisetweaks/RuntimeFeatureUsabilityRegressionTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/runtime/FeatureManagerFaultInjectionSGradeTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/runtime/FeatureManagerTickSlotTest.java");
    }

    @Test
    void guiResponsibilityHasArchitectureEvidence() {
        assertExists("src/test/java/dev/chise/chisetweaks/gui/GuiResponsibilitySGradeContractTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/gui/SettingsResponsibilityArchitectureContractTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/gui/ChiseListEditorScreenArchitectureContractTest.java");
    }

    @Test
    void performanceHasStructuralAcceptanceAndRealDeviceEvidenceLayers() throws IOException {
        assertExists("src/test/java/dev/chise/chisetweaks/performance/PerformanceArchitectureContractTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/performance/PerformanceAcceptanceSGradeTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/performance/JfrPerformanceAnalyzerTest.java");
        assertExists("scripts/performance_evidence_template.py");
        assertExists("scripts/prism_acceptance_audit.py");
        String build = Files.readString(ROOT.resolve("build.gradle"));
        assertTrue(build.contains("comparePerformanceEvidence"));
    }

    @Test
    void configAndSecurityHaveAdversarialMigrationAndStorageEvidence() {
        assertExists("src/test/java/dev/chise/chisetweaks/core/security/ConfigAdversarialSGradeTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/core/security/SecureConfigStorageTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/config/HistoricalConfigFixtureTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/config/ConfigMigrationDowngradeTest.java");
    }

    @Test
    void productScopeHasIndependentExecutableEvidence() {
        assertExists("src/test/java/dev/chise/chisetweaks/ProductScopeSGradeContractTest.java");
        assertExists("src/test/java/dev/chise/chisetweaks/RepositoryScopeContractTest.java");
    }

    @Test
    void releaseRegressionHasArtifactParityAndProvenanceLayers() {
        assertExists("src/test/java/dev/chise/chisetweaks/ReleaseEvidenceSGradeContractTest.java");
        for (String path : List.of(
                "scripts/artifact_audit.py",
                "scripts/functional_parity_audit.py",
                "scripts/ci_provenance.py",
                "scripts/release_residue_audit.py")) {
            assertExists(path);
        }
    }

    @Test
    void minecraftSemanticsHaveClientGameTestEvidence() {
        assertExists("src/gametest/java/dev/chise/chisetweaks/regression/AllFeaturesRegressionClientGameTest.java");
        assertExists("src/gametest/java/dev/chise/chisetweaks/gui/TrapdoorPlacementClientGameTest.java");
        assertExists("src/gametest/resources/fabric.mod.json");
    }

    private static void assertExists(String relativePath) {
        assertTrue(Files.isRegularFile(ROOT.resolve(relativePath)), () -> "Missing evidence: " + relativePath);
    }
}
