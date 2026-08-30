package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HandheldSizeRenderingContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void implementationScalesOnlyTheExistingFirstPersonItemSubmitPath() throws Exception {
        String mixin = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/HandheldSizeMixin.java"));
        String policy = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/vision/HandheldSizePolicy.java"));

        assertTrue(mixin.contains("@Mixin(ItemInHandRenderer.class)"));
        assertTrue(mixin.contains("method = \"renderItem\""));
        assertTrue(mixin.contains("poseStack.pushPose()"));
        assertTrue(mixin.contains("poseStack.scale(scale, scale, scale)"));
        assertTrue(mixin.contains("poseStack.popPose()"));
        assertTrue(policy.contains("ItemDisplayContext.FIRST_PERSON_RIGHT_HAND"));
        assertTrue(policy.contains("ItemDisplayContext.FIRST_PERSON_LEFT_HAND"));

        for (String forbidden : new String[]{
                "ResourceManager", "ResourcePack", "reloadResourcePacks",
                "ItemModelResolver.updateForTopItem", "setBlock(", "sendPacket",
                "KeyMapping.click(", "new Thread("}) {
            assertFalse(mixin.contains(forbidden), forbidden);
        }
    }

    @Test
    void runtimeResourcesDoNotCopyVanillaSmallHandheldModelsOrTextures() throws Exception {
        Path vanillaNamespace = ROOT.resolve("src/main/resources/assets/minecraft");
        if (!Files.exists(vanillaNamespace)) return;
        try (var paths = Files.walk(vanillaNamespace)) {
            assertFalse(paths.anyMatch(Files::isRegularFile),
                    "Handheld Size must remain code-based; do not copy vanilla model/texture replacements");
        }
    }
}
