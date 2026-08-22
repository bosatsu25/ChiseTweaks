package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CI検証、SemVer、利用者が導入する単一JARの境界を固定する。 */
final class ReleaseDistributionContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void normalCiVerifiesOnlyAndNeverCreatesTagsOrReleases() throws IOException {
        String ci = source(".github/workflows/ci.yml");

        assertTrue(ci.contains("uses: ./.github/workflows/verify-build.yml"));
        assertTrue(ci.contains("cancel-in-progress: true"));
        assertFalse(ci.contains("contents: write"));
        assertFalse(ci.contains("gh release"));
        assertFalse(ci.contains("git tag"));
        assertFalse(ci.contains("verified-v"));
        assertFalse(ci.contains("Publish verified runtime JAR"));
    }

    @Test
    void verificationDoesNotUploadZipArchivesOrPublishQaEvidence() throws IOException {
        String verify = source(".github/workflows/verify-build.yml");

        assertTrue(verify.contains("python scripts/version_policy.py"));
        assertTrue(verify.contains("./gradlew --no-daemon --stacktrace clean ciGate"));
        assertTrue(verify.contains("python scripts/artifact_audit.py"));
        assertTrue(verify.contains("python scripts/visual_asset_audit.py"));
        assertTrue(verify.contains("python scripts/release_residue_audit.py"));
        assertFalse(verify.contains("actions/upload-artifact"));
        assertFalse(verify.contains("verification-evidence"));
        assertFalse(verify.contains("build/verified"));
    }

    @Test
    void officialReleaseFollowsSuccessfulMainCiAndUploadsExactlyOneRuntimeJar() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("workflow_run:"));
        assertTrue(release.contains("github.event.workflow_run.conclusion == 'success'"));
        assertTrue(release.contains("github.event.workflow_run.event == 'push'"));
        assertTrue(release.contains("github.event.workflow_run.head_branch == 'main'"));
        assertTrue(release.contains("ref: ${{ github.event.workflow_run.head_sha }}"));
        assertTrue(release.contains("./gradlew --no-daemon --stacktrace clean ciGate"));
        assertTrue(release.contains("python scripts/version_policy.py"));
        assertTrue(release.contains("gh release create \"$TAG\""));
        assertTrue(release.contains("\"build/libs/$RUNTIME_JAR\""));
        assertTrue(release.contains("Official Release must expose exactly one uploaded mod asset"));
        assertFalse(release.contains("actions/upload-artifact"));
        assertFalse(release.contains("\"build/libs/$SOURCES_JAR\""));
        assertFalse(release.contains("\"build/ci/SHA256SUMS.txt\""));
        assertFalse(release.contains("\"build/ci/artifact-audit.json\""));
        assertFalse(release.contains("\"build/ci/visual-asset-audit.json\""));
        assertFalse(release.contains("\"build/ci/quality-summary.md\""));
    }

    @Test
    void legacyPerCommitVerifiedTagsAreReadOnlyVersionFallbacks() throws IOException {
        String release = source(".github/workflows/release.yml");

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

        String policy = source("VERSIONING.md");
        String script = source("scripts/version_policy.py");
        assertTrue(policy.contains("PATCH"));
        assertTrue(policy.contains("MINOR"));
        assertTrue(policy.contains("MAJOR"));
        assertTrue(policy.contains("Every official release must increase the SemVer core"));
        assertTrue(script.contains("expected_bump"));
        assertTrue(script.contains("--release-type"));
        assertTrue(script.contains("current_minecraft != minecraft"));
    }

    @Test
    void releaseNotesTellPrismUsersToInstallOnlyRuntimeJar() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("Install / 導入"));
        assertTrue(release.contains("Prism Launcher の Mods には **\\`${RUNTIME_JAR}\\`** だけを追加してください"));
        assertTrue(release.contains("- Minecraft:"));
        assertTrue(release.contains("- Fabric Loader:"));
        assertTrue(release.contains("- Fabric API:"));
        assertTrue(release.contains("- Java:"));
        assertTrue(release.contains("- SHA-256:"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
