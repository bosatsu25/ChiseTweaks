package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaIntegrationArchitectureContractTest {
    @Test
    void externalModsRemainSoftDependenciesAndMixinsRemainFailSoft() throws Exception {
        String fabric = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String mixins = Files.readString(Path.of("src/main/resources/chisetweaks.integrations.mixins.json"));

        for (String mod : new String[]{"litematica", "tweakeroo", "tweakermore", "syncmatica", "malilib"}) {
            assertFalse(fabric.contains("\"" + mod + "\":"), "Masa mod became metadata dependency: " + mod);
        }
        assertTrue(fabric.contains("chisetweaks.integrations.mixins.json"));
        assertTrue(mixins.contains("\"required\": false"));
        assertTrue(mixins.contains("\"defaultRequire\": 0"));
        assertTrue(mixins.contains("IntegrationMixinConfigPlugin"));
    }

    @Test
    void externalVersionDriftStaysBehindOptionalMixinAndReflectionBoundaries() throws Exception {
        String reflection = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/integration/masa/MasaReflectionSupport.java"));
        String plugin = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/mixin/IntegrationMixinConfigPlugin.java"));

        assertTrue(reflection.contains("ReflectiveOperationException | LinkageError"));
        assertTrue(plugin.contains("MasaModAvailability.isLoaded(modId)"));

        for (String path : new String[]{
                "src/main/java/dev/chise/chisetweaks/mixin/masa/MaLiLibTranslationMixin.java",
                "src/main/java/dev/chise/chisetweaks/mixin/masa/LitematicaMaterialCacheMixin.java",
                "src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerooToolSwitchMixin.java",
                "src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerMoreAutoPickMixin.java",
                "src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerMoreMaterialRefreshMixin.java",
                "src/main/java/dev/chise/chisetweaks/mixin/masa/SyncmaticaRemoveListenerMixin.java"}) {
            String source = Files.readString(Path.of(path));
            assertTrue(source.contains("@Pseudo"), path);
            assertTrue(source.contains("require = 0"), path);
        }
    }

    @Test
    void syncmaticaGuardDoesNotTakePacketOwnership() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/mixin/masa/SyncmaticaRemoveListenerMixin.java"));
        assertFalse(source.contains("sendPacket"));
        assertFalse(source.contains("PacketByteBuf"));
        assertFalse(source.contains("REMOVE_SYNCMATIC"));
    }
}
