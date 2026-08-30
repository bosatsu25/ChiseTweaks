package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestResponsibilityContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void eachRiskClassHasAnExecutableVerificationOwner() {
        Map<String, List<String>> responsibility = new LinkedHashMap<>();
        responsibility.put("pure-policy", List.of(
                "src/test/java/dev/chise/chisetweaks/RetainedPolicyQualityGateTest.java",
                "src/test/java/dev/chise/chisetweaks/core/policy/FeatureAvailabilityPolicyTest.java"));
        responsibility.put("config-security", List.of(
                "src/test/java/dev/chise/chisetweaks/core/security/SecureConfigStorageTest.java",
                "src/test/java/dev/chise/chisetweaks/config/ConfigDocumentPolicyTest.java"));
        responsibility.put("runtime-lifecycle", List.of(
                "src/test/java/dev/chise/chisetweaks/runtime/FeatureManagerTickSlotTest.java",
                "src/test/java/dev/chise/chisetweaks/runtime/ClientSessionLifecycleContractTest.java"));
        responsibility.put("ui-contract", List.of(
                "src/test/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsControllerTest.java",
                "src/test/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsLayoutTest.java"));
        responsibility.put("performance-evidence", List.of(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceArchitectureContractTest.java",
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceComparisonTest.java"));
        responsibility.put("minecraft-oracle", List.of(
                "src/gametest/java/dev/chise/chisetweaks/gui/TrapdoorPlacementClientGameTest.java",
                "src/gametest/java/dev/chise/chisetweaks/regression/AllFeaturesRegressionClientGameTest.java"));
        responsibility.put("distribution-parity", List.of(
                "scripts/functional_parity_audit.py",
                "scripts/artifact_audit.py",
                "quality/functional-parity-baseline.json"));

        for (var entry : responsibility.entrySet()) {
            for (String relative : entry.getValue()) {
                assertTrue(Files.isRegularFile(ROOT.resolve(relative)),
                        () -> entry.getKey() + " has no executable owner: " + relative);
            }
        }
    }

    @Test
    void ciRunsBothDeterministicUnitGateAndMinecraftOracle() throws Exception {
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        assertTrue(ci.contains("./gradlew --stacktrace ciGate"));
        assertTrue(ci.contains("./gradlew --stacktrace runClientGameTest"));
        assertTrue(ci.contains("python scripts/functional_parity_audit.py"));
        assertTrue(ci.contains("python scripts/artifact_audit.py"));
    }
}
