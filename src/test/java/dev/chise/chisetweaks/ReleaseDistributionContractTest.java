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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CI検証、SemVer、利用者が導入する単一JARの境界を固定する。 */
final class ReleaseDistributionContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void normalCiVerifiesOnlyAndNeverCreatesTagsOrReleases() throws IOException {
        String ci = read(".github/workflows/ci.yml");

        assertContainsAll(ci,
                "uses: ./.github/workflows/verify-build.yml",
                "cancel-in-progress: true");
        assertContainsNone(ci,
                "contents: write",
                "gh release",
                "git tag",
                "verified-v",
                "Publish verified runtime JAR");
    }

    @Test
    void verificationExposesIndependentGatesWithoutPublishingEvidence() throws IOException {
        String verify = read(".github/workflows/verify-build.yml");

        assertContainsAll(verify,
                "python scripts/version_policy.py",
                "clean assemble testClasses",
                "--stacktrace test",
                "jacocoTestReport jacocoTestCoverageVerification",
                "--stacktrace pitest",
                "python scripts/artifact_audit.py",
                "python scripts/visual_asset_audit.py",
                "python scripts/release_residue_audit.py",
                "python scripts/quality_summary.py --allow-partial",
                "Enforce aggregate quality gate");
        assertContainsNone(verify,
                "actions/upload-artifact",
                "verification-evidence",
                "build/verified",
                "gh release",
                "git tag");
    }

    @Test
    void officialReleaseKeepsTheStrictSingleCiGateAndUploadsExactlyOneRuntimeJar() throws IOException {
        String release = read(".github/workflows/release.yml");

        assertContainsAll(release,
                "workflow_run:",
                "github.event.workflow_run.conclusion == 'success'",
                "github.event.workflow_run.event == 'push'",
                "github.event.workflow_run.head_branch == 'main'",
                "ref: ${{ github.event.workflow_run.head_sha }}",
                "./gradlew --no-daemon --stacktrace clean ciGate",
                "python scripts/version_policy.py",
                "gh release create \"$TAG\"",
                "\"build/libs/$RUNTIME_JAR\"",
                "Official Release must expose exactly one uploaded mod asset");
        assertContainsNone(release,
                "actions/upload-artifact",
                "\"build/libs/$SOURCES_JAR\"",
                "\"build/ci/SHA256SUMS.txt\"",
                "\"build/ci/artifact-audit.json\"",
                "\"build/ci/visual-asset-audit.json\"",
                "\"build/ci/quality-summary.md\"");
    }

    @Test
    void legacyPerCommitVerifiedTagsAreReadOnlyVersionFallbacks() throws IOException {
        String release = read(".github/workflows/release.yml");

        assertTrue(release.contains("git tag --list 'verified-v*'"));
        assertFalse(release.contains("gh release delete \"$legacy_tag\""));
        assertFalse(release.contains("git push origin \":refs/tags/$legacy_tag\""));
        assertFalse(release.contains("tag=\"verified-v"));
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
