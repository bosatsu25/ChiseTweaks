package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.integration.masa.MasaJapaneseUiMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MasaJapaneseTextPolicyTest {
    @Test
    void autoOnlyActivatesForJapaneseMinecraftLanguage() {
        int auto = MasaJapaneseUiMode.AUTO.id();
        assertTrue(MasaJapaneseTextPolicy.effectiveJapanese(auto, "ja_jp"));
        assertTrue(MasaJapaneseTextPolicy.effectiveJapanese(auto, "JA_JP"));
        assertFalse(MasaJapaneseTextPolicy.effectiveJapanese(auto, "en_us"));
        assertFalse(MasaJapaneseTextPolicy.effectiveJapanese(auto, null));
    }

    @Test
    void explicitModeOverridesMinecraftLanguage() {
        assertTrue(MasaJapaneseTextPolicy.effectiveJapanese(
                MasaJapaneseUiMode.ENABLED.id(), "en_us"));
        assertFalse(MasaJapaneseTextPolicy.effectiveJapanese(
                MasaJapaneseUiMode.DISABLED.id(), "ja_jp"));
    }

    @Test
    void knownLabelsKeepEnglishSearchTermsAndUnknownTextStaysUntouched() {
        assertEquals(
                "一般設定 (Generic)",
                MasaJapaneseTextPolicy.translate(
                        "malilib.gui.title.generic",
                        "Generic",
                        MasaJapaneseUiMode.ENABLED.id(),
                        "en_us"));
        assertEquals(
                "配置制限 (placementRestriction)",
                MasaJapaneseTextPolicy.translate(
                        "litematica.config.generic.name.placementRestriction",
                        "placementRestriction",
                        MasaJapaneseUiMode.ENABLED.id(),
                        "en_us"));
        assertEquals(
                "Upstream custom text",
                MasaJapaneseTextPolicy.translate(
                        "unknown.key",
                        "Upstream custom text",
                        MasaJapaneseUiMode.ENABLED.id(),
                        "ja_jp"));
    }

    @Test
    void disabledModeNeverRewritesKnownUpstreamText() {
        assertEquals(
                "Remove",
                MasaJapaneseTextPolicy.translate(
                        "syncmatica.gui.button.remove",
                        "Remove",
                        MasaJapaneseUiMode.DISABLED.id(),
                        "ja_jp"));
    }
}
