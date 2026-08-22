package dev.chise.chisetweaks.feature.resource;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTexturePackContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PACK_ROOT =
            ROOT.resolve("src/main/resources/resourcepacks/chise_texture");

    @Test
    void registersAsAUserControllableDefaultEnabledBuiltinPack() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackFeature.java"));
        String client = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java"));
        String metadata = Files.readString(PACK_ROOT.resolve("pack.mcmeta"));

        assertTrue(source.contains("ResourceLoader.registerBuiltinPack"));
        assertTrue(source.contains("PackActivationType.DEFAULT_ENABLED"));
        assertTrue(source.contains("Identifier.fromNamespaceAndPath"));
        assertTrue(client.contains("SafeStartup.run(\"chise-texture-pack\", ChiseTexturePackFeature::register)"));
        assertTrue(metadata.contains("\"min_format\": 84"));
        assertTrue(metadata.contains("\"max_format\": 84"));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/minecraft")));
    }

    @Test
    void keepsUploadedGameplayTexturesByteExactAndUsesSmallPackIcon() throws Exception {
        assertPng(
                "assets/minecraft/textures/block/white_concrete.png",
                16,
                16,
                "9002be7c6d6a1e0cda4a056f503d7992110defadda4722f85c21a0656d79b5f4");
        assertPng(
                "assets/minecraft/textures/entity/chest/normal.png",
                64,
                64,
                "3eceee354a860e9248ebb445c4791e41cbc5fa7d05168da68f3d1a7a735cf0e0");
        assertPng(
                "assets/minecraft/textures/entity/chest/normal_left.png",
                64,
                64,
                "d062c5ae62eb9cac74434fe6874ae6c2fc88eda822fd2a8ac406649c42e42d62");
        assertPng(
                "assets/minecraft/textures/entity/chest/normal_right.png",
                64,
                64,
                "22bed5c9a841c9d403c666850a9bd84ab131e616a93e205d7d5442e3f70b17be");

        BufferedImage packIcon = ImageIO.read(PACK_ROOT.resolve("pack.png").toFile());
        assertNotNull(packIcon);
        assertEquals(64, packIcon.getWidth());
        assertEquals(64, packIcon.getHeight());
    }

    @Test
    void artifactAuditAllowsOnlyTheSingleKnownBuiltinPack() throws IOException {
        String audit = Files.readString(ROOT.resolve("scripts/artifact_audit.py"));
        String core = Files.readString(ROOT.resolve("scripts/artifact_audit_core.py"));

        assertTrue(audit.contains("CHISE_TEXTURE_PACK_ROOT = \"resourcepacks/chise_texture/\""));
        assertTrue(audit.contains("resource_pack_files != CHISE_TEXTURE_PACK_FILES"));
        assertTrue(audit.contains("name.startswith(\"shaderpacks/\")"));
        assertTrue(audit.contains("names_without_builtin_pack"));
        assertTrue(audit.contains("_ORIGINAL_AUDIT_ORE_HIGHLIGHTS"));
        assertTrue(audit.contains("unexpected_shader_or_resource_packs_bundled"));
        assertFalse(audit.contains("resource_pack_files.issubset"));
        assertTrue(core.contains("runtime JAR unexpectedly bundles shader/resource packs"));
        assertTrue(core.contains("legacy Ore Highlights replacement models returned"));
    }

    private static void assertPng(String relativePath, int width, int height, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
        Path path = PACK_ROOT.resolve(relativePath);
        byte[] bytes = Files.readAllBytes(path);
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image);
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
        assertEquals(
                expectedSha256,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }
}
