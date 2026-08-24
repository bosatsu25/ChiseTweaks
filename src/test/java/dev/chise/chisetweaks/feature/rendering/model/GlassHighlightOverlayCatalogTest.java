package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GlassHighlightOverlayCatalogTest {
    @Test
    void shapeLookupPreservesBlockPaneAndRejectedInputContracts() {
        assertSame(GlassHighlightOverlayCatalog.BLOCK_KEY,
                GlassHighlightOverlayCatalog.keyFor(GlassHighlightTargetPolicy.Shape.BLOCK));
        assertSame(GlassHighlightOverlayCatalog.PANE_KEY,
                GlassHighlightOverlayCatalog.keyFor(GlassHighlightTargetPolicy.Shape.PANE));
        assertThrows(IllegalArgumentException.class,
                () -> GlassHighlightOverlayCatalog.keyFor(GlassHighlightTargetPolicy.Shape.NONE));
        assertThrows(NullPointerException.class,
                () -> GlassHighlightOverlayCatalog.keyFor(null));
    }
}
