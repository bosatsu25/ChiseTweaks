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
    void syncmaticaGuardDoesNotTakePacketOwnership() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/mixin/masa/SyncmaticaRemoveListenerMixin.java"));
        assertFalse(source.contains("sendPacket"));
        assertFalse(source.contains("PacketByteBuf"));
        assertFalse(source.contains("REMOVE_SYNCMATIC"));
    }
}
