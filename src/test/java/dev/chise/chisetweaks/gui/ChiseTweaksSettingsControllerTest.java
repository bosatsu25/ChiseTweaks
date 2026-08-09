package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.FeatureSwitches;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsControllerTest {
    private final ChiseTweaksSettingsController controller =
            new ChiseTweaksSettingsController(true);

    @Test
    void visibilityScreenKeepsTheApprovedRowOrderAndSectionLabels() {
        List<ChiseTweaksSettingRowDefinition> rows =
                controller.rowsFor(ChiseTweaksUiSection.VISIBILITY);

        assertEquals(List.of(
                "header.visibility",
                "thread",
                "hidden",
                "glass",
                "header.hiddenTargets",
                "visualTargetHiddenBlueIce",
                "visualTargetHiddenDeadCoral",
                "visualTargetHiddenPowderSnow",
                "visualTargetHiddenSculkCatalyst",
                "header.sceneFilter",
                "focusBlocks",
                "focusEntities",
                "header.visibilityDetails",
                "lava",
                "scanRange",
                "scanInterval"), ids(rows));

        assertEquals("見やすさ", row(rows, "header.visibility").name());
        assertEquals("見えにくいブロックの対象", row(rows, "header.hiddenTargets").name());
        assertEquals("表示を絞る対象", row(rows, "header.sceneFilter").name());
        assertEquals("溶岩・視認の詳細設定", row(rows, "header.visibilityDetails").name());
        assertEquals("細線トレース", row(rows, "thread").name());
        assertEquals("隠面トレース", row(rows, "hidden").name());
        assertEquals("ガラス検査", row(rows, "glass").name());
        assertEquals("視認スキャン範囲", row(rows, "scanRange").name());
        assertEquals("スキャン間隔", row(rows, "scanInterval").name());
    }

    @Test
    void placementAndResourcePagesKeepTheirFixedTargetFamilyCounts() {
        List<ChiseTweaksSettingRowDefinition> placement =
                controller.rowsFor(ChiseTweaksUiSection.PLACEMENT);
        List<ChiseTweaksSettingRowDefinition> resources =
                controller.rowsFor(ChiseTweaksUiSection.RESOURCES);

        assertEquals(11, countIdsStartingWith(placement, "visualTargetPlacement"));
        assertEquals(13, countIdsStartingWith(resources, "visualTargetMaterial"));
        assertEquals("設置・向き", row(placement, "header.placement").name());
        assertEquals("設置方向ガイドの対象", row(placement, "header.placementTargets").name());
        assertEquals("資源", row(resources, "header.resources").name());
        assertEquals("ハイライト対象", row(resources, "header.resourceTargets").name());
    }

    @Test
    void keybindPageContainsOnlyAHeaderAndExplicitEditActions() {
        List<ChiseTweaksSettingRowDefinition> rows =
                controller.rowsFor(ChiseTweaksUiSection.HOTKEYS);

        assertEquals(FeatureSwitches.VALUES.size() + 1, rows.size());
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.HEADER, rows.get(0).kind());
        assertEquals("キー設定", rows.get(0).name());
        for (int index = 1; index < rows.size(); index++) {
            assertEquals(ChiseTweaksSettingRowDefinition.Kind.ACTION, rows.get(index).kind());
            assertFalse(rows.get(index).id().isBlank());
        }
    }

    @Test
    void categoryRowsRemainCompactTypedControlsWithoutSoloInteractionMode() {
        for (ChiseTweaksUiSection section : List.of(
                ChiseTweaksUiSection.PLACEMENT,
                ChiseTweaksUiSection.RESOURCES,
                ChiseTweaksUiSection.VISIBILITY)) {
            for (ChiseTweaksSettingRowDefinition row : controller.rowsFor(section)) {
                assertTrue(row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                        || row.kind() == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN
                        || row.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER);
                String combined = (row.id() + " " + row.name() + " " + row.description()).toLowerCase();
                assertFalse(combined.contains("solo"));
                assertFalse(combined.contains("none"));
            }
        }
    }

    @Test
    void nullOrGuideSectionHasNoSettingsRows() {
        assertTrue(controller.rowsFor(null).isEmpty());
        assertTrue(controller.rowsFor(ChiseTweaksUiSection.HELP).isEmpty());
    }

    private static List<String> ids(List<ChiseTweaksSettingRowDefinition> rows) {
        return rows.stream().map(ChiseTweaksSettingRowDefinition::id).toList();
    }

    private static ChiseTweaksSettingRowDefinition row(
            List<ChiseTweaksSettingRowDefinition> rows,
            String id) {
        return rows.stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static long countIdsStartingWith(
            List<ChiseTweaksSettingRowDefinition> rows,
            String prefix) {
        return rows.stream().filter(row -> row.id().startsWith(prefix)).count();
    }
}
