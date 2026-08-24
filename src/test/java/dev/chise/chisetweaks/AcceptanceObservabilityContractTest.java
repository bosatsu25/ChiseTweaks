package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AcceptanceObservabilityContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void prismAuditSeparatesTransportDisconnectsFromChiseFailures() throws IOException {
        String audit = source("scripts/prism_acceptance_audit.py");
        assertTrue(audit.contains("transport_disconnect_not_chise_failure"));
        assertTrue(audit.contains("MixinTransformerError"));
        assertTrue(audit.contains("resource-reload-terminal-failure"));
        assertTrue(audit.contains("client-startup"));
        assertTrue(audit.contains("client-join"));
        assertTrue(audit.contains("client-disconnect"));
    }

    @Test
    void performanceTemplatesCoverEveryRequiredPrismScenario() throws IOException {
        String template = source("scripts/performance_evidence_template.py");
        String guide = source("DEVELOPMENT.md");
        for (String scenario : new String[] {
                "chise-absent",
                "chise-all-off",
                "analyzers-on",
                "highlights-on",
                "maximum-supported-load"}) {
            assertTrue(template.contains('"' + scenario + '"'), scenario);
            assertTrue(guide.contains(scenario), scenario);
        }
        assertTrue(template.contains("p50_frametime_ms"));
        assertTrue(template.contains("p95_frametime_ms"));
        assertTrue(template.contains("p99_frametime_ms"));
        assertTrue(template.contains("heap_mib"));
        assertTrue(template.contains("average_fps"));
        assertTrue(guide.contains("comparePerformanceEvidence"));
    }

    @Test
    void automatedRuntimeContractsCoverReloadAnalyzerRendererAndJarRegression() throws IOException {
        String runtime = source("scripts/runtime_performance_contract_audit.py");
        String compatibility = source("scripts/compatibility_contract_audit.py");
        String artifact = source("scripts/artifact_audit.py");
        String properties = source("gradle.properties");

        assertTrue(runtime.contains("resource reload callers changed"));
        assertTrue(runtime.contains("MAX_VALIDATION_CHUNKS_PER_TICK = 16"));
        assertTrue(runtime.contains("MAX_TRACKED_CHUNKS = 4096"));
        assertTrue(compatibility.contains("FORBIDDEN_METADATA_RELATIONS = (\"depends\", \"breaks\", \"conflicts\")"));
        assertTrue(compatibility.contains("runtime_performance_contract_audit.audit()"));
        assertTrue(artifact.contains("runtime_jar_target_bytes"));
        assertTrue(artifact.contains("runtime_jar_baseline_bytes"));
        assertTrue(artifact.contains("runtime_jar_max_growth_bytes"));
        assertTrue(properties.contains("runtime_jar_target_bytes=358400"));
        assertTrue(properties.contains("runtime_jar_baseline_bytes=446814"));
        assertTrue(properties.contains("runtime_jar_max_growth_bytes=0"));
    }

    @Test
    void acceptanceGuideExplicitlyKeepsPhysicalPrismChecksSeparateFromCi() throws IOException {
        String guide = source("DEVELOPMENT.md");
        assertTrue(guide.contains("11"));
        assertTrue(guide.contains("Chest"));
        assertTrue(guide.contains("White Concrete"));
        assertTrue(guide.contains("Ancient Debris"));
        assertTrue(guide.contains("disconnect"));
        assertTrue(guide.contains("prism_acceptance_audit.py"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
