package dev.chise.chisetweaks.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorTranslationContractTest {
    private static final Path LANG = Path.of("src/main/resources/assets/chisetweaks/lang");
    private static final List<String> KEYS = List.of(
            "screen.chisetweaks.inspector.title",
            "screen.chisetweaks.inspector.no_target",
            "screen.chisetweaks.inspector.no_target.description",
            "screen.chisetweaks.inspector.target.block",
            "screen.chisetweaks.inspector.target.entity",
            "screen.chisetweaks.inspector.block_state",
            "screen.chisetweaks.inspector.state.orientation",
            "screen.chisetweaks.inspector.state.shape",
            "screen.chisetweaks.inspector.state.connection",
            "screen.chisetweaks.inspector.state.interaction",
            "screen.chisetweaks.inspector.state.fluid",
            "screen.chisetweaks.inspector.state.other",
            "screen.chisetweaks.inspector.filter",
            "screen.chisetweaks.inspector.filter.visible",
            "screen.chisetweaks.inspector.filter.hidden",
            "screen.chisetweaks.inspector.matched_rule",
            "screen.chisetweaks.inspector.responsible_feature",
            "screen.chisetweaks.inspector.render_mode",
            "screen.chisetweaks.inspector.render_mode.visible",
            "screen.chisetweaks.inspector.render_mode.through_wall",
            "screen.chisetweaks.inspector.render_mode.suppressed",
            "screen.chisetweaks.inspector.none",
            "screen.chisetweaks.inspector.reason.filter_off",
            "screen.chisetweaks.inspector.reason.no_rule",
            "screen.chisetweaks.inspector.reason.hide_list_match",
            "screen.chisetweaks.inspector.reason.hide_list_no_match",
            "screen.chisetweaks.inspector.reason.allow_list_match",
            "screen.chisetweaks.inspector.reason.allow_list_no_match",
            "screen.chisetweaks.inspector.reason.self_protected",
            "screen.chisetweaks.inspector.reason.unregistered",
            "screen.chisetweaks.placement.title",
            "screen.chisetweaks.placement.impossible",
            "screen.chisetweaks.placement.predicted",
            "screen.chisetweaks.placement.actual",
            "screen.chisetweaks.placement.awaiting_actual",
            "screen.chisetweaks.placement.result",
            "screen.chisetweaks.placement.result.match",
            "screen.chisetweaks.placement.result.adjusted",
            "screen.chisetweaks.placement.result.different",
            "screen.chisetweaks.placement.result.unavailable",
            "screen.chisetweaks.placement.changed",
            "screen.chisetweaks.placement.changed.other",
            "screen.chisetweaks.placement.reason.upper",
            "screen.chisetweaks.placement.reason.lower",
            "screen.chisetweaks.settings.help.show",
            "screen.chisetweaks.settings.help.hide",
            "screen.chisetweaks.help.title",
            "screen.chisetweaks.help.highlight.name",
            "screen.chisetweaks.help.highlight.description",
            "screen.chisetweaks.help.filter.name",
            "screen.chisetweaks.help.filter.description",
            "screen.chisetweaks.help.analyzer.name",
            "screen.chisetweaks.help.analyzer.description",
            "screen.chisetweaks.help.visibility.name",
            "screen.chisetweaks.help.visibility.description",
            "screen.chisetweaks.help.settings.name",
            "screen.chisetweaks.help.settings.description",
            "screen.chisetweaks.help.troubleshooting.name",
            "screen.chisetweaks.help.troubleshooting.description");

    @Test
    void englishAndJapaneseContainEveryInspectorKey() throws Exception {
        for (String locale : List.of("en_us", "ja_jp")) {
            JsonObject language = JsonParser.parseString(
                    Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
            for (String key : KEYS) {
                assertTrue(language.has(key), () -> locale + " missing " + key);
                assertTrue(!language.get(key).getAsString().isBlank(), () -> locale + " blank " + key);
            }
        }
    }
}
