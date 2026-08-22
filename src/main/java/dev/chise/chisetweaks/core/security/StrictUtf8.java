package dev.chise.chisetweaks.core.security;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** 不正なバイト列や孤立サロゲートを拒否する厳格なUTF-8変換処理。 */
public final class StrictUtf8 {
    private StrictUtf8() {
    }

    public static String decode(byte[] bytes) throws IOException {
        if (bytes == null) throw new IOException("UTF-8 input is missing");
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException malformed) {
            throw new IOException("input is not strict UTF-8", malformed);
        }
    }

    public static byte[] encode(String value) throws IOException {
        if (value == null) throw new IOException("UTF-8 text is missing");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } catch (CharacterCodingException malformed) {
            throw new IOException("text contains malformed Unicode", malformed);
        }
    }
}
