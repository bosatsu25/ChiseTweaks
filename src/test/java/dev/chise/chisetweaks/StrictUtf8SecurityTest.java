package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.security.StrictUtf8;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class StrictUtf8SecurityTest {
    @Test
    void roundTripsValidUnicodeWithoutReplacementCharacters() throws Exception {
        String value = "ChiseTweaks 日本語 🧱";
        byte[] encoded = StrictUtf8.encode(value);

        assertEquals(value, StrictUtf8.decode(encoded));
    }

    @Test
    void rejectsMissingInputs() {
        assertThrows(IOException.class, () -> StrictUtf8.decode(null));
        assertThrows(IOException.class, () -> StrictUtf8.encode(null));
    }

    @Test
    void rejectsMalformedAndTruncatedUtf8() {
        assertThrows(IOException.class, () -> StrictUtf8.decode(new byte[]{(byte) 0xC3, 0x28}));
        assertThrows(IOException.class, () -> StrictUtf8.decode(new byte[]{(byte) 0xE2, (byte) 0x82}));
        assertThrows(IOException.class, () -> StrictUtf8.decode(new byte[]{(byte) 0xC0, (byte) 0xAF}));
        assertThrows(IOException.class, () -> StrictUtf8.decode(new byte[]{(byte) 0x80}));
    }

    @Test
    void rejectsUnpairedSurrogateInput() {
        assertThrows(IOException.class, () -> StrictUtf8.encode("\uD800"));
        assertThrows(IOException.class, () -> StrictUtf8.encode("\uDC00"));
        assertDoesNotThrow(() -> StrictUtf8.encode("\uD83E\uDDF1"));
    }
}
