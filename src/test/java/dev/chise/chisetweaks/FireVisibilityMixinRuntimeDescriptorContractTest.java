package dev.chise.chisetweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the Minecraft 26.1.2 renderFire descriptor used by Fire Visibility. */
final class FireVisibilityMixinRuntimeDescriptorContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void minecraftRenderFireDescriptorMatchesTheMixinContract() throws Exception {
        Method renderFire = ScreenEffectRenderer.class.getDeclaredMethod(
                "renderFire",
                PoseStack.class,
                MultiBufferSource.class,
                TextureAtlasSprite.class);

        assertNotNull(renderFire);
        assertEquals(void.class, renderFire.getReturnType());
        assertTrue(Modifier.isStatic(renderFire.getModifiers()));
    }

    @Test
    void bothMixinHandlersCarryTheTextureAtlasSpriteArgument() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/FireVisibilityMixin.java"));

        assertTrue(source.contains("import net.minecraft.client.renderer.texture.TextureAtlasSprite;"));
        assertEquals(2, occurrences(source, "TextureAtlasSprite sprite,"));
        assertTrue(source.contains("@Inject(method = \"renderFire\", at = @At(\"HEAD\"))"));
        assertTrue(source.contains("@Inject(method = \"renderFire\", at = @At(\"RETURN\"))"));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = text.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }
}
