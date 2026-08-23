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
    private static final Path CHEST_PACK =
            ROOT.resolve("src/main/resources/resourcepacks/chise_chest_visibility");
    private static final Path WHITE_CONCRETE_PACK =
            ROOT.resolve("src/main/resources/resourcepacks/chise_white_concrete_visibility");

    @Test
    void registersTwoIndependentDefaultEnabledBuiltinPacks() throws IOException {
        String registrar = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackRegistrar.java"));
        String packType = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/VisibilityPack.java"));
        String client = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java"));

        assertTrue(packType.contains("CHEST(\"chise_chest_visibility\", \"Bright Chest\")"));
        assertTrue(packType.contains("WHITE_CONCRETE(\"chise_white_concrete_visibility\", \"Bright Concrete\")"));
        assertTrue(registrar.contains("for (VisibilityPack pack : VisibilityPack.values())"));
        assertTrue(registrar.contains("ResourceLoader.registerBuiltinPack"));
        assertTrue(registrar.contains("PackActivationType.DEFAULT_ENABLED"));
        assertTrue(client.contains("SafeStartup.run(\"chise-texture-pack\", ChiseTexturePackRegistrar::register)"));
        assertTrue(client.contains("SafeStartup.run(\"visibility-pack-migration\""));
        assertTrue(Files.readString(CHEST_PACK.resolve("pack.mcmeta")).contains("\"min_format\": 84"));
        assertTrue(Files.readString(WHITE_CONCRETE_PACK.resolve("pack.mcmeta")).contains("\"max_format\": 84"));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_texture")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/minecraft")));
    }

    @Test
    void gameplayTexturesRemainByteExactAndPhysicallySeparated() throws Exception {
        assertPng(
                WHITE_CONCRETE_PACK.resolve("assets/minecraft/textures/block/white_concrete.png"),
                16, 16,
                "9002be7c6d6a1e0cda4a056f503d7992110defadda4722f85c21a0656d79b5f4");
        assertPng(
                CHEST_PACK.resolve("assets/minecraft/textures/entity/chest/normal.png"),
                64, 64,
                "3eceee354a860e9248ebb445c4791e41cbc5fa7d05168da68f3d1a7a735cf0e0");
        assertPng(
                CHEST_PACK.resolve("assets/minecraft/textures/entity/chest/normal_left.png"),
                64, 64,
                "d062c5ae62eb9cac74434fe6874ae6c2fc88eda822fd2a8ac406649c42e42d62");
        assertPng(
                CHEST_PACK.resolve("assets/minecraft/textures/entity/chest/normal_right.png"),
                64, 64,
                "22bed5c9a841c9d403c666850a9bd84ab131e616a93e205d7d5442e3f70b17be");

        assertFalse(Files.exists(CHEST_PACK.resolve("assets/minecraft/textures/block/white_concrete.png")));
        assertFalse(Files.exists(WHITE_CONCRETE_PACK.resolve("assets/minecraft/textures/entity/chest/normal.png")));
        assertPackIcon(CHEST_PACK);
        assertPackIcon(WHITE_CONCRETE_PACK);
    }

    @Test
    void controllerUsesOneSerializedDelayedTextureReloadPipelineForBothPacks() throws IOException {
        String controller = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java"));
        String coordinator = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ResourceReloadCoordinator.java"));
        assertTrue(controller.contains("isEnabled(VisibilityPack pack)"));
        assertTrue(controller.contains("setEnabled(VisibilityPack pack, boolean enabled)"));
        assertTrue(controller.contains("ResourceReloadCoordinator RELOADS"));
        assertTrue(controller.contains("RELOADS.markPending(selected)"));
        assertTrue(controller.contains("client.delayTextureReload().whenComplete"));
        assertFalse(controller.contains("client.reloadResourcePacks().whenComplete"));
        assertTrue(controller.contains("ResourcePackSelectionPolicy.withPack"));
        assertTrue(controller.contains("restoreSelection"));
        assertTrue(controller.contains("TERMINAL_RECOVERY"));
        assertTrue(coordinator.contains("record Recovery"));
        assertTrue(coordinator.contains("terminalFailure"));
        assertTrue(coordinator.contains("cancel"));
        assertTrue(coordinator.contains("RELOAD"));
        assertTrue(coordinator.contains("RESTORE"));
    }

    @Test
    void brightPackSettingsUseMinecraftOptionsInsteadOfUnrelatedApplyPersistence() throws IOException {
        String chest = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java"));
        String concrete = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java"));
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));
        assertTrue(chest.contains("requiresApplyPersistence()"));
        assertTrue(chest.contains("return false;"));
        assertTrue(concrete.contains("requiresApplyPersistence()"));
        assertTrue(screen.contains("config.requiresApplyPersistence()"));
        assertTrue(screen.contains("public void tick()"));
        assertTrue(screen.contains("refreshRowButtons();"));
    }

    @Test
    void migrationDefersCompletionMarkerUntilChangedSelectionIsReobservedNextStartup() throws IOException {
        String migration = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/resource/VisibilityPackMigrationService.java"));
        assertTrue(migration.contains("chisetweaks:chise_texture"));
        assertTrue(migration.contains("options.txt"));
        assertTrue(migration.contains("chisetweaks-visibility-pack-migration-v1.txt"));
        assertTrue(migration.contains("VisibilityPackMigrationPolicy.plan"));
        assertTrue(migration.contains("VisibilityPack.CHEST.repositoryPackId()"));
        assertTrue(migration.contains("VisibilityPack.WHITE_CONCRETE.repositoryPackId()"));

        int changed = migration.indexOf("if (plan.selectionChanged())");
        int apply = migration.indexOf("applyMigrationSelection", changed);
        int deferred = migration.indexOf("marker-awaits-next-startup", apply);
        int markerWrite = migration.indexOf("SecureConfigStorage.writeUtf8Atomic(configDir, MARKER_FILE", deferred);
        assertTrue(changed >= 0);
        assertTrue(apply > changed);
        assertTrue(deferred > apply);
        assertTrue(markerWrite > deferred);
    }

    @Test
    void artifactAuditAllowsExactlyTheTwoKnownBuiltinPacks() throws IOException {
        String audit = Files.readString(ROOT.resolve("scripts/artifact_audit.py"));
        String core = Files.readString(ROOT.resolve("scripts/artifact_audit_core.py"));

        assertTrue(audit.contains("CHEST_PACK_ROOT = \"resourcepacks/chise_chest_visibility/\""));
        assertTrue(audit.contains("WHITE_CONCRETE_PACK_ROOT = \"resourcepacks/chise_white_concrete_visibility/\""));
        assertTrue(audit.contains("resource_pack_files != VISIBILITY_PACK_FILES"));
        assertTrue(audit.contains("name.startswith(\"shaderpacks/\")"));
        assertTrue(audit.contains("names_without_builtin_packs"));
        assertFalse(audit.contains("resource_pack_files.issubset"));
        assertTrue(core.contains("runtime JAR unexpectedly bundles shader/resource packs"));
    }

    private static void assertPackIcon(Path packRoot) throws IOException {
        BufferedImage packIcon = ImageIO.read(packRoot.resolve("pack.png").toFile());
        assertNotNull(packIcon);
        assertEquals(64, packIcon.getWidth());
        assertEquals(64, packIcon.getHeight());
    }

    private static void assertPng(Path path, int width, int height, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
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
