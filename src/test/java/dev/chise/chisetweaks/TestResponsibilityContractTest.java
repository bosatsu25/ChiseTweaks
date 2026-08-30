package dev.chise.chisetweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestResponsibilityContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path RISK_REGISTER = ROOT.resolve("quality/risk-register.json");
    private static final Set<String> QUALITY_CHARACTERISTICS = Set.of(
            "functional_suitability",
            "performance_efficiency",
            "compatibility",
            "usability",
            "reliability",
            "security",
            "maintainability");
    private static final Set<String> TEST_TECHNIQUES = Set.of(
            "boundary_value",
            "checklist_based",
            "combination_test",
            "decision_table",
            "domain_test",
            "dynamic_analysis",
            "fault_injection",
            "operational_profile",
            "scenario_based",
            "state_transition",
            "static_analysis");
    private static final Set<String> ORDINAL = Set.of("low", "medium", "high");
    private static final Set<String> PRIORITY = Set.of("low", "medium", "high", "critical");

    @Test
    void productRisksTraceToTechniqueOracleEvidenceAndExitCriteria() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(RISK_REGISTER)).getAsJsonObject();
        assertEquals(1, root.get("schema_version").getAsInt());

        JsonArray risks = root.getAsJsonArray("risks");
        assertTrue(risks.size() >= 6, "risk register must cover the major retained product risks");

        Set<String> ids = new HashSet<>();
        for (var element : risks) {
            JsonObject risk = element.getAsJsonObject();
            String id = requiredText(risk, "id");
            assertTrue(ids.add(id), () -> "duplicate risk id: " + id);
            requiredText(risk, "title");

            assertTrue(ORDINAL.contains(requiredText(risk, "likelihood")),
                    () -> id + " has unsupported likelihood");
            assertTrue(ORDINAL.contains(requiredText(risk, "impact")),
                    () -> id + " has unsupported impact");
            assertTrue(PRIORITY.contains(requiredText(risk, "priority")),
                    () -> id + " has unsupported priority");

            JsonArray characteristics = requiredArray(risk, "quality_characteristics");
            assertFalse(characteristics.size() == 0, () -> id + " needs a quality characteristic");
            for (var characteristic : characteristics) {
                assertTrue(QUALITY_CHARACTERISTICS.contains(characteristic.getAsString()),
                        () -> id + " has unknown quality characteristic: " + characteristic);
            }

            JsonArray techniques = requiredArray(risk, "test_techniques");
            assertFalse(techniques.size() == 0, () -> id + " needs an explicit test technique");
            for (var technique : techniques) {
                assertTrue(TEST_TECHNIQUES.contains(technique.getAsString()),
                        () -> id + " has unknown test technique: " + technique);
            }

            requiredText(risk, "oracle");
            JsonArray evidence = requiredArray(risk, "evidence");
            assertFalse(evidence.size() == 0, () -> id + " has no executable evidence");
            for (var evidencePath : evidence) {
                Path relative = Path.of(evidencePath.getAsString()).normalize();
                assertFalse(relative.isAbsolute(), () -> id + " evidence must be repository-relative");
                assertFalse(relative.startsWith(".."), () -> id + " evidence escapes repository root");
                assertTrue(Files.isRegularFile(ROOT.resolve(relative)),
                        () -> id + " evidence does not exist: " + relative);
            }

            JsonArray exitCriteria = requiredArray(risk, "exit_criteria");
            assertFalse(exitCriteria.size() == 0, () -> id + " has no exit criteria");
            for (var criterion : exitCriteria) {
                assertFalse(criterion.getAsString().isBlank(), () -> id + " has a blank exit criterion");
            }

            String priority = risk.get("priority").getAsString();
            if ("high".equals(priority) || "critical".equals(priority)) {
                assertTrue(evidence.size() >= 2,
                        () -> id + " high-priority risk needs independent evidence layers");
            }
        }
    }

    @Test
    void ciRunsDeterministicGateMinecraftOracleAndDistributionAudits() throws Exception {
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        assertTrue(ci.contains("./gradlew --stacktrace ciGate"));
        assertTrue(ci.contains("./gradlew --stacktrace runClientGameTest"));
        assertTrue(ci.contains("python scripts/functional_parity_audit.py"));
        assertTrue(ci.contains("python scripts/artifact_audit.py"));
    }

    private static JsonArray requiredArray(JsonObject object, String name) {
        assertTrue(object.has(name) && object.get(name).isJsonArray(), () -> "missing array: " + name);
        return object.getAsJsonArray(name);
    }

    private static String requiredText(JsonObject object, String name) {
        assertTrue(object.has(name) && object.get(name).isJsonPrimitive(), () -> "missing text: " + name);
        String value = object.get(name).getAsString();
        assertFalse(value.isBlank(), () -> "blank text: " + name);
        return value;
    }
}
