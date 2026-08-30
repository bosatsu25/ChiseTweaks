package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S8: source verification and GitHub Release must remain one provenance-preserving evidence chain. */
final class ReleaseEvidenceSGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void pullRequestEvidenceRecordsTheExactTestedTreeAndRuntimeDigest() throws IOException {
        String ci = workflow();

        assertTrue(ci.contains("tested_tree=\"$(git rev-parse 'HEAD^{tree}')\""));
        assertTrue(ci.contains("--tested-tree-sha \"$tested_tree\""));
        assertTrue(ci.contains("--runtime-verified --runtime-jar \"$RUNTIME_JAR\" --runtime-sha256 \"$RUNTIME_SHA256\""));
        assertTrue(ci.contains("name: chise-ci-provenance"));
        assertTrue(ci.contains("path: build/ci/ci-provenance.json"));
    }

    @Test
    void mainReuseIsFailClosedOnTreeOrArtifactMismatch() throws IOException {
        String ci = workflow();

        assertTrue(ci.contains("current_tree=\"$(git rev-parse 'HEAD^{tree}')\""));
        assertTrue(ci.contains("--current-tree-sha \"$current_tree\""));
        assertTrue(ci.contains("reason=direct-or-unverified-main"));
        assertTrue(ci.contains("scope=full"));
        assertTrue(ci.contains("heavy=true"));
    }

    @Test
    void releasePromotesTheVerifiedJarInsteadOfRebuilding() throws IOException {
        String ci = workflow();
        int releaseStart = ci.indexOf("  release:\n");
        assertTrue(releaseStart >= 0, "release job is missing");
        String release = ci.substring(releaseStart);

        assertTrue(release.contains("name: release / Publish verified runtime JAR"));
        assertTrue(release.contains("name: ${{ needs.verify.outputs.runtime_jar }}"));
        assertTrue(release.contains("Download exact CI artifact"));
        assertTrue(release.contains("sha256sum \"$runtime\""));
        assertTrue(release.contains("unzip -p \"$runtime\" fabric.mod.json"));
        assertTrue(release.contains("gh release create"));

        assertFalse(release.contains("./gradlew"), "Release job must never rebuild the runtime");
        assertFalse(release.contains("gradle/actions/setup-gradle"));
        assertFalse(release.contains("actions/setup-java"));
    }

    @Test
    void staleMainRunsCannotPublish() throws IOException {
        String ci = workflow();

        assertTrue(ci.contains("latest_main_sha=\"$(gh api \"repos/${GH_REPO}/commits/main\" --jq '.sha')\""));
        assertTrue(ci.contains("if [[ \"$latest_main_sha\" != \"$TARGET_SHA\" ]]"));
        assertTrue(ci.contains("should_release=false"));
    }

    private static String workflow() throws IOException {
        return Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
    }
}
