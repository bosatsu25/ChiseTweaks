package dev.chise.chisetweaks.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureConfigStandaloneRoundTripTest {
    @AfterEach
    void resetGlobalSettings() {
        FeatureConfig.readFromJson(new JsonObject());
    }

    @Test
    void standaloneWriterAndReaderRoundTripFeatureAndSceneFilterState() {
        FeatureSwitches.PLACEMENT_GUIDE.setBooleanValueSilently(true);
        FeatureSwitches.MATERIAL_HIGHLIGHTS.setBooleanValueSilently(true);
        FeatureSwitches.GLASS_INSPECTION.setBooleanValueSilently(false);
        BuilderFocusConfig.REFRESH_RENDERER.setBooleanValueSilently(false);
        BuilderFocusConfig.BLOCK_RULE_MODE.setValueSilently(ChiseRuleMode.WHITELIST);
        BuilderFocusConfig.BLOCK_WHITELIST.setStringsSilently(List.of(
                "minecraft:stone", "minecraft:glass"));
        BuilderFocusConfig.BLOCK_BLACKLIST.setStringsSilently(List.of());
        BuilderFocusConfig.ENTITY_RULE_MODE.setValueSilently(ChiseRuleMode.BLACKLIST);
        BuilderFocusConfig.ENTITY_BLACKLIST.setStringsSilently(List.of(
                "minecraft:item", "minecraft:experience_orb"));
        BuilderFocusConfig.ENTITY_WHITELIST.setStringsSilently(List.of());

        JsonObject persisted = FeatureConfig.writeToJson();

        FeatureConfig.readFromJson(new JsonObject());
        assertFalse(FeatureSwitches.PLACEMENT_GUIDE.getBooleanValue());
        assertEquals(ChiseRuleMode.NONE, BuilderFocusConfig.BLOCK_RULE_MODE.getValue());

        FeatureConfig.readFromJson(persisted);

        assertTrue(FeatureSwitches.PLACEMENT_GUIDE.getBooleanValue());
        assertTrue(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
        assertFalse(FeatureSwitches.GLASS_INSPECTION.getBooleanValue());
        assertFalse(BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue());
        assertEquals(ChiseRuleMode.WHITELIST, BuilderFocusConfig.BLOCK_RULE_MODE.getValue());
        assertEquals(List.of("minecraft:stone", "minecraft:glass"),
                BuilderFocusConfig.BLOCK_WHITELIST.getStrings());
        assertEquals(ChiseRuleMode.BLACKLIST, BuilderFocusConfig.ENTITY_RULE_MODE.getValue());
        assertEquals(List.of("minecraft:item", "minecraft:experience_orb"),
                BuilderFocusConfig.ENTITY_BLACKLIST.getStrings());
    }

    @Test
    void legacyHotkeysAreIgnoredAndNeverWrittenByStandaloneConfig() {
        JsonObject legacy = JsonParser.parseString("""
                {
                  "FeatureToggles": {
                    "materialHighlights": true
                  },
                  "FeatureHotkeys": {
                    "materialHighlights": "KEY_M"
                  }
                }
                """).getAsJsonObject();

        FeatureConfig.readFromJson(legacy);
        JsonObject rewritten = FeatureConfig.writeToJson();

        assertTrue(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
        assertFalse(rewritten.has("FeatureHotkeys"));
        assertTrue(rewritten.has("FeatureToggles"));
    }

    @Test
    void malformedListItemsAndUnknownRuleModeFailClosedWithoutDroppingKnownToggle() {
        JsonObject source = JsonParser.parseString("""
                {
                  "FeatureToggles": {
                    "placementGuide": true
                  },
                  "Lists": {
                    "builderFocusBlockRuleMode": "future_mode",
                    "builderFocusBlockWhitelist": ["minecraft:stone", 42, true, " minecraft:glass "]
                  }
                }
                """).getAsJsonObject();

        FeatureConfig.readFromJson(source);

        assertTrue(FeatureSwitches.PLACEMENT_GUIDE.getBooleanValue());
        assertEquals(ChiseRuleMode.NONE, BuilderFocusConfig.BLOCK_RULE_MODE.getValue());
        assertEquals(List.of("minecraft:stone", "minecraft:glass"),
                BuilderFocusConfig.BLOCK_WHITELIST.getStrings());
    }
}
