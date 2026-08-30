package dev.chise.chisetweaks.integration.masa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MasaJapaneseUiModeTest {
    @Test
    void malformedModeFallsBackToAuto() {
        assertEquals(MasaJapaneseUiMode.AUTO, MasaJapaneseUiMode.fromId(-100));
        assertEquals(MasaJapaneseUiMode.AUTO, MasaJapaneseUiMode.fromId(100));
        assertEquals(MasaJapaneseUiMode.ENABLED, MasaJapaneseUiMode.fromId(1));
        assertEquals(MasaJapaneseUiMode.DISABLED, MasaJapaneseUiMode.fromId(2));
    }
}
