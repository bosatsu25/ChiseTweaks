package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CI検証、SemVer、利用者が導入する単一JARの境界を固定する。 */
final class ReleaseDistributionContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void normalCiIsOneReadOnlyFailFastQualityPipeline() throws IOException {
        String ci = read(".github/workflows/ci.yml");

        assertContainsAll(ci,
                "permissions:\n  contents: read",
                "name: verify / Java 25 quality gate",
                "python scripts/version_policy.py",
                "python scripts/repository_audit.py",
                "python scripts/functional_parity_audit.py",
                "./gradlew --stacktrace ciGate",
                "python scripts/artifact_audit.py",
                "python scripts/visual_asset_audit.py",
                "python scripts/release_residue_audit.py",
                "actions/upload-artifact@",
                "path: build/libs/${{ steps.artifacts.outputs.runtime_jar }}",
                "archive: false",
                "cancel-in-progress: true");
        assertContainsNone(ci,
                "uses: ./.github/workflows/verify-build.yml",
                "continue-on-error",
                "Enforce aggregate quality gate",
                "quality_summary.py",
                "clean assemble testClasses",
                "--stacktrace test",
                "jacocoTestReport jacocoTestCoverageVerification",
                "--stacktrace pitest",
                "contents: write",
                "gh release",
                "git tag");
    }

    @Test
    void officialReleasePromotesTheExactSuccessfulMainCiArtifact() throws IOException {
        String release = read(".github/workflows/release.yml");

        assertContainsAll(release,
                "workflow_run:",
                "github.event.workflow_run.conclusion == 'success'",
                "github.event.workflow_run.event == 'push'",
                "github.event.workflow_run.head_branch == 'main'",
                "actions: read",
                "contents: write",
                "actions/download-artifact@",
                "run-id: ${{ github.event.workflow_run.id }}",
                "github-token: ${{ github.token }}",
                "pattern: chise-tweaks-*.jar",
                "Validate promoted runtime JAR",
                "sha256sum \"$runtime\"",
                "unzip -p \"$runtime\" fabric.mod.json",
                "serverInstallationRequired",
                "gh release create \"$TAG\"",
                "Published runtime JAR digest mismatch",
                "Release tag points to");
        assertContainsNone(release,
                "actions/checkout@",
                "actions/setup-java@",
                "actions/setup-python@",
                "gradle/actions/setup-gradle@",
                "./gradlew",
                "python scripts/artifact_audit.py",
                "python scripts/visual_asset_audit.py",
                "python scripts/release_residue_audit.py",
                "gh release download",
                "actions/upload-artifact@");
    }

    @Test
    void newOfficialReleaseKeepsTheOneStepSemverGuardWithoutRebuilding() throws IOException {
        String release = read(".github/workflows/release.yml");
        String policy = read("VERSIONING.md");

        assertContainsAll(release,
                "Enforce one-step SemVer increment",
                "gh release list",
                "release_type='patch'",
                "release_type='minor'",
                "release_type='major'",
                "not an exact patch/minor/major increment");
        assertContainsAll(policy,
                "promotes the exact runtime JAR retained by that successful CI run",
                "Only one-step PATCH, MINOR, or MAJOR increments are accepted");
        assertContainsNone(release,
                "python scripts/version_policy.py",
                "actions/checkout@",
                "./gradlew");
    }

    @Test
    void versionFormatKeepsSemverCoreAndMinecraftMetadataTogether() throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(ROOT.resolve("gradle.properties"))) {
            properties.load(reader);
        }
        String version = properties.getProperty("mod_version");
        String minecraft = properties.getProperty("minecraft_version");
        assertTrue(version.matches("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\+mc"
                + Pattern.quote(minecraft)));

        String policy = read("VERSIONING.md");
        String script = read("scripts/version_policy.py");
        assertContainsAll(policy,
                "PATCH",
                "MINOR",
                "MAJOR",
                "Every official release must increase the SemVer core");
        assertContainsAll(script,
                "expected_bump",
                "--release-type",
                "current_minecraft != minecraft");
    }

    @Test
    void releaseNotesTellPrismUsersToInstallOnlyRuntimeJar() throws IOException {
        String release = read(".github/workflows/release.yml");

        assertContainsAll(release,
                "Install / 導入",
                "Prism Launcher の Mods には **\\`${RUNTIME_JAR}\\`** だけを追加してください",
                "- Minecraft:",
                "- Fabric Loader:",
                "- Fabric API:",
                "- Java:",
                "- SHA-256:");
    }
}
