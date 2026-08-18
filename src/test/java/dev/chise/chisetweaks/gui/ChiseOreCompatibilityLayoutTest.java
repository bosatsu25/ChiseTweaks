package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseOreCompatibilityLayoutTest {
    @Test
    void desktopLayoutKeepsSingleRowControlsInsidePanel() {
        var layout = ChiseOreCompatibilityLayout.calculate(1280, 720);
        assertFalse(layout.compact());
        assertContained(layout);
        assertFalse(layout.idInput().overlaps(layout.style()));
        assertFalse(layout.style().overlaps(layout.add()));
    }

    @Test
    void narrowGuiScaleUsesCompactTwoRowControlsWithoutHorizontalOverflow() {
        var layout = ChiseOreCompatibilityLayout.calculate(320, 240);
        assertTrue(layout.compact());
        assertContained(layout);
        assertFalse(layout.idInput().overlaps(layout.style()));
        assertFalse(layout.idInput().overlaps(layout.add()));
        assertFalse(layout.style().overlaps(layout.add()));
        assertTrue(layout.pageSize() >= 1);
    }

    @Test
    void veryNarrowPolicyStillKeepsInteractiveRectsInsideThePanel() {
        var layout = ChiseOreCompatibilityLayout.calculate(220, 200);
        assertTrue(layout.compact());
        assertContained(layout);
        assertTrue(layout.removeWidth() > 0);
    }

    private static void assertContained(ChiseOreCompatibilityLayout.Geometry layout) {
        var panel = layout.panel();
        assertHorizontalContainment(panel, layout.idInput());
        assertHorizontalContainment(panel, layout.style());
        assertHorizontalContainment(panel, layout.add());
        assertHorizontalContainment(panel, layout.previous());
        assertHorizontalContainment(panel, layout.next());
        assertHorizontalContainment(panel, layout.clear());
        assertHorizontalContainment(panel, layout.back());
        assertFalse(layout.previous().overlaps(layout.next()));
        assertFalse(layout.next().overlaps(layout.clear()));
        assertFalse(layout.clear().overlaps(layout.back()));
    }

    private static void assertHorizontalContainment(
            ChiseOreCompatibilityLayout.Rect panel,
            ChiseOreCompatibilityLayout.Rect child) {
        assertTrue(child.x() >= panel.x());
        assertTrue(child.right() <= panel.right());
    }
}
