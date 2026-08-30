package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.gui.ChiseTweaksSettingsController.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * User-facing product composition for the Tweaks-oriented settings model.
 *
 * <p>Existing settings and persistence bindings are reused deliberately: this class changes
 * product grouping without renaming config keys or coupling runtime components together.</p>
 */
final class TweaksProductSettingsRows {
    private TweaksProductSettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        Surface resolved = surface == null ? Surface.HIGHLIGHT : surface;
        return switch (resolved) {
            case HIGHLIGHT -> builderHighlights();
            case FILTER -> relabel(
                    ChiseTweaksSettingsRows.rows(Surface.FILTER),
                    "product.sceneFilter",
                    title(Surface.FILTER));
            case ANALYZER -> technicalVisualization();
            case VISIBILITY -> visualTweaks();
            case INTEGRATIONS -> relabel(
                    ChiseTweaksSettingsRows.rows(Surface.INTEGRATIONS),
                    "product.integrations",
                    title(Surface.INTEGRATIONS));
            case INSPECTOR -> List.of();
        };
    }

    static String title(Surface surface) {
        Surface resolved = surface == null ? Surface.HIGHLIGHT : surface;
        return switch (resolved) {
            case HIGHLIGHT -> localized("Builder Highlights", "建築ハイライト");
            case FILTER -> localized("Scene Filter", "表示フィルター");
            case INSPECTOR -> localized("Builder Assist", "建築アシスト");
            case ANALYZER -> localized("Technical", "技術可視化");
            case VISIBILITY -> localized("Visual Tweaks", "表示調整");
            case INTEGRATIONS -> localized("Integrations", "連携");
        };
    }

    private static List<ChiseTweaksSettingRowDefinition> builderHighlights() {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        rows.add(ChiseTweaksSettingRowDefinition.header("product.builderHighlights", title(Surface.HIGHLIGHT)));

        append(rows, ChiseTweaksSettingsRows.rows(Surface.HIGHLIGHT), row -> {
            String id = row.id();
            if ("header.highlight".equals(id)) return false;
            if ("thread".equals(id)) return false;
            if (id.startsWith("fineThread")) return false;
            if (id.startsWith("visualTargetTechnical")) return false;
            if ("detail.highlight.traceAppearance".equals(id)) return false;
            if ("detail.highlight.technicalTargets".equals(id)) return false;
            if (isSharedOverlayBudget(id)) return false;
            return true;
        });

        append(rows, ChiseTweaksSettingsRows.rows(Surface.ANALYZER), row -> {
            String id = row.id();
            return !"header.analyzer".equals(id) && !"villagerAnalyzer".equals(id);
        });
        return List.copyOf(rows);
    }

    private static List<ChiseTweaksSettingRowDefinition> technicalVisualization() {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        rows.add(ChiseTweaksSettingRowDefinition.header("product.technical", title(Surface.ANALYZER)));

        append(rows, ChiseTweaksSettingsRows.rows(Surface.HIGHLIGHT), row -> {
            String id = row.id();
            return "thread".equals(id)
                    || id.startsWith("fineThread")
                    || id.startsWith("visualTargetTechnical")
                    || "detail.highlight.traceAppearance".equals(id)
                    || "detail.highlight.technicalTargets".equals(id)
                    || isSharedOverlayBudget(id);
        });

        append(rows, ChiseTweaksSettingsRows.rows(Surface.VISIBILITY), row ->
                "beaconRange".equals(row.id()) || "lightningRodRange".equals(row.id()));
        append(rows, ChiseTweaksSettingsRows.rows(Surface.ANALYZER), row ->
                "villagerAnalyzer".equals(row.id()));
        return List.copyOf(rows);
    }

    private static List<ChiseTweaksSettingRowDefinition> visualTweaks() {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        rows.add(ChiseTweaksSettingRowDefinition.header("product.visualTweaks", title(Surface.VISIBILITY)));
        append(rows, ChiseTweaksSettingsRows.rows(Surface.VISIBILITY), row -> {
            String id = row.id();
            return !"header.visibility".equals(id)
                    && !"beaconRange".equals(id)
                    && !"lightningRodRange".equals(id);
        });
        return List.copyOf(rows);
    }

    private static List<ChiseTweaksSettingRowDefinition> relabel(
            List<ChiseTweaksSettingRowDefinition> source,
            String headerId,
            String title) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        rows.add(ChiseTweaksSettingRowDefinition.header(headerId, title));
        boolean skippedHeader = false;
        for (ChiseTweaksSettingRowDefinition row : source) {
            if (!skippedHeader && row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                skippedHeader = true;
                continue;
            }
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private static void append(
            ArrayList<ChiseTweaksSettingRowDefinition> target,
            List<ChiseTweaksSettingRowDefinition> source,
            Predicate<ChiseTweaksSettingRowDefinition> keep) {
        for (ChiseTweaksSettingRowDefinition row : source) {
            if (keep.test(row)) target.add(row);
        }
    }

    private static boolean isSharedOverlayBudget(String id) {
        return "detail.highlight.general".equals(id)
                || "highlightRange".equals(id)
                || "highlightVerticalRange".equals(id)
                || "highlightInterval".equals(id)
                || "highlightMaxOverlays".equals(id)
                || "highlightWorldOverlay".equals(id)
                || "highlightDimensionPresets".equals(id);
    }

    private static String localized(String english, String japanese) {
        return "ja".equals(ChiseTweaksSettingsRows.text("screen.chisetweaks.help.language.probe"))
                ? japanese
                : english;
    }
}
