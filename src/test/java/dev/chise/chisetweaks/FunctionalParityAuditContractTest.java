package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 350KiB化で機能契約を暗黙に削らないためのM0 baseline契約。 */
final class FunctionalParityAuditContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenBaselineCoversTheProductContractsRequiredForSizeOptimization() throws IOException {
        String baseline = source("quality/functional-parity-baseline.json");

        assertTrue(baseline.contains("\"version\": \"0.9.4+mc26.1.2\""));
        assertTrue(baseline.contains("\"runtimeJarBytes\": 446814"));
        assertTrue(baseline.contains("\"goalBytes\": 358400"));
        assertTrue(baseline.contains("\"featureIds\""));
        assertTrue(baseline.contains("\"globalFeatureSwitches\""));
        assertTrue(baseline.contains("\"localFeatureSwitches\""));
        assertTrue(baseline.contains("\"localSettings\""));
        assertTrue(baseline.contains("\"visualTargets\""));
        assertTrue(baseline.contains("\"builderFocus\""));
        assertTrue(baseline.contains("\"resourcePackIds\""));
        assertTrue(baseline.contains("\"mixins\""));
        assertTrue(baseline.contains("\"entrypoints\""));
        assertTrue(baseline.contains("\"depends\""));
        assertTrue(baseline.contains("\"uiActions\""));
        assertTrue(baseline.contains("\"diagnosticEvents\""));
        assertTrue(baseline.contains("\"migrationFixtures\""));
        assertTrue(baseline.contains("\"analyzerBudget\""));
        assertTrue(baseline.contains("\"serverInstallationRequired\": false"));
    }

    @Test
    void parityAuditIsARequiredCiRepositoryGate() throws IOException {
        String audit = source("scripts/functional_parity_audit.py");
        String workflow = source(".github/workflows/verify-build.yml");

        assertTrue(workflow.contains("python scripts/functional_parity_audit.py"));
        assertTrue(audit.contains("FUNCTIONAL PARITY AUDIT: PASS"));
        assertTrue(audit.contains("feature ids"));
        assertTrue(audit.contains("global feature switches"));
        assertTrue(audit.contains("local setting defaults"));
        assertTrue(audit.contains("visual target defaults"));
        assertTrue(audit.contains("built-in resource pack ids"));
        assertTrue(audit.contains("Mixin list"));
        assertTrue(audit.contains("Fabric client-only custom contract"));
        assertTrue(audit.contains("UI actions"));
        assertTrue(audit.contains("diagnostic events"));
        assertTrue(audit.contains("migration fixtures missing"));
        assertTrue(audit.contains("Ancient Debris analyzer budget"));
        assertTrue(audit.contains("Update the baseline only for an intentional, reviewed product-contract change."));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
