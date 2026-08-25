package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnifiedFeatureSwitchRuntimeBindingTest {
    @Test
    void allFifteenSwitchesRemainIndependentWhenEnabledTogether() {
        List<FeatureSwitch> switches = FeatureSwitches.VALUES;
        assertEquals(15, switches.size());

        ArrayList<Boolean> original = new ArrayList<>(switches.size());
        for (FeatureSwitch feature : switches) original.add(feature.getBooleanValue());
        try {
            for (FeatureSwitch feature : switches) feature.setBooleanValueSilently(true);
            for (FeatureSwitch feature : switches) assertTrue(feature.getBooleanValue(), feature.getName());

            for (FeatureSwitch target : switches) {
                target.setBooleanValueSilently(false);
                assertFalse(target.getBooleanValue(), target.getName());
                for (FeatureSwitch other : switches) {
                    if (other != target) assertTrue(other.getBooleanValue(), other.getName());
                }
                target.setBooleanValueSilently(true);
            }
        } finally {
            for (int index = 0; index < switches.size(); index++) {
                switches.get(index).setBooleanValueSilently(original.get(index));
            }
        }
    }

    @Test
    void localFeatureSwitchesStillBindToTheirRuntimeFields() {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        boolean fire = local.fireVisibilityEnabled;
        boolean lava = local.lavaHighlightEnabled;
        boolean debris = local.ancientDebrisAnalyzerEnabled;
        boolean warden = local.wardenRiskAnalyzerEnabled;
        boolean chest = local.brightChestEnabled;
        boolean concrete = local.brightConcreteEnabled;
        try {
            FeatureSwitches.FIRE_VISIBILITY.setBooleanValueSilently(true);
            FeatureSwitches.LAVA_HIGHLIGHT.setBooleanValueSilently(true);
            FeatureSwitches.ANCIENT_DEBRIS_ANALYZER.setBooleanValueSilently(true);
            FeatureSwitches.WARDEN_RISK_ANALYZER.setBooleanValueSilently(true);
            FeatureSwitches.BRIGHT_CHEST.setBooleanValueSilently(false);
            FeatureSwitches.BRIGHT_CONCRETE.setBooleanValueSilently(false);

            assertTrue(local.fireVisibilityEnabled);
            assertTrue(local.lavaHighlightEnabled);
            assertTrue(local.ancientDebrisAnalyzerEnabled);
            assertTrue(local.wardenRiskAnalyzerEnabled);
            assertFalse(local.brightChestEnabled);
            assertFalse(local.brightConcreteEnabled);
        } finally {
            local.fireVisibilityEnabled = fire;
            local.lavaHighlightEnabled = lava;
            local.ancientDebrisAnalyzerEnabled = debris;
            local.wardenRiskAnalyzerEnabled = warden;
            local.brightChestEnabled = chest;
            local.brightConcreteEnabled = concrete;
        }
    }
}
