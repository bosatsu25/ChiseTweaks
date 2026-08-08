package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ResidualPolicyCoverageTest {
    @Test
    void slabTopTypeMapsToTopHalfWhenHalfPropertyIsAbsent() {
        var overlay = OrientationOverlayPolicy.inspect(Map.of("type", "top"));
        assertEquals(OrientationOverlayPolicy.Half.TOP, overlay.half());
    }
}
