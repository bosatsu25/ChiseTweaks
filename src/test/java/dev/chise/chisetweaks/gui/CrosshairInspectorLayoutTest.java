package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.world.phys.HitResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorLayoutTest {
    @Test
    void inspectorRowsWrapInsideThreeSupportedGuiWidths() {
        var snapshot = new CrosshairInspector.Snapshot(
                HitResult.Type.BLOCK,
                "minecraft:oak_trapdoor",
                List.of(
                        "facing=north", "half=top", "open=false", "powered=false",
                        "waterlogged=true", "custom_property=safe_value"),
                new BuilderFocusVisibility.FilterDecision(
                        true,
                        BuilderFocusVisibility.REASON_HIDE_LIST_MATCH,
                        "minecraft:oak_trapdoor"),
                List.of(
                        FeatureDefinition.FINE_THREAD_TRACE,
                        FeatureDefinition.MATERIAL_HIGHLIGHTS));
        List<ChiseTweaksSettingRowDefinition> rows =
                new ChiseTweaksSettingsCatalog().inspectorRows(snapshot, true);

        for (int width : List.of(320, 480, 960)) {
            var geometry = ChiseTweaksSettingsLayout.calculate(width, 480);
            for (ChiseTweaksSettingRowDefinition row : rows) {
                if (row.kind() != ChiseTweaksSettingRowDefinition.Kind.INFO) continue;
                var layout = ChiseTweaksInfoTextLayout.create(
                        row.name(),
                        row.description(),
                        geometry.infoTextWidth(),
                        9,
                        geometry.infoRowHeight(),
                        CrosshairInspectorLayoutTest::wrap);
                assertTrue(layout.textBottom() <= layout.rowHeight());
                for (String line : layout.nameLines()) {
                    assertTrue(pixelWidth(line) <= geometry.infoTextWidth(), line);
                }
                for (String line : layout.descriptionLines()) {
                    assertTrue(pixelWidth(line) <= geometry.infoTextWidth(), line);
                }
            }
        }
    }

    private static List<String> wrap(String text, int availableWidth) {
        ArrayList<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\n", -1)) {
            StringBuilder line = new StringBuilder();
            int width = 0;
            for (int offset = 0; offset < paragraph.length();) {
                int codePoint = paragraph.codePointAt(offset);
                int characterWidth = pixelWidth(codePoint);
                if (!line.isEmpty() && width + characterWidth > availableWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                    width = 0;
                }
                line.appendCodePoint(codePoint);
                width += characterWidth;
                offset += Character.charCount(codePoint);
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return List.copyOf(lines);
    }

    private static int pixelWidth(String text) {
        return text.codePoints().map(CrosshairInspectorLayoutTest::pixelWidth).sum();
    }

    private static int pixelWidth(int codePoint) {
        return codePoint <= 0x7F ? 6 : 9;
    }
}
