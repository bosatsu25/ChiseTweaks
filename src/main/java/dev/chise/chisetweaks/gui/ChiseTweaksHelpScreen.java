package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.core.definition.FeatureHelpLevel;
import dev.chise.chisetweaks.gui.help.FeatureHelpCatalog;
import dev.chise.chisetweaks.gui.help.FeatureHelpDisplayLanguage;
import dev.chise.chisetweaks.gui.help.FeatureHelpEntry;
import dev.chise.chisetweaks.gui.help.FeatureHelpLanguageCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Japanese-first, searchable in-game guide using the same five-section navigation as settings. */
public final class ChiseTweaksHelpScreen extends Screen {
    private static final int NAV_Y = 26;
    private static final int SEARCH_Y = 52;
    private static final int TOP = 82;
    private static final int BOTTOM = 34;
    private static final int CARD_GAP = 8;
    private static final int CARD_PADDING = 8;
    private static final int LINE_HEIGHT = 11;

    private final Screen parent;
    private FeatureHelpDisplayLanguage displayLanguage = initialLanguage();
    private Button japaneseButton;
    private Button englishButton;
    private Button backButton;
    private EditBox searchBox;
    private String searchQuery = "";
    private int scrollOffset;
    private int maxScroll;

    public ChiseTweaksHelpScreen(Screen parent) {
        super(Component.translatable("screen.chisetweaks.help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        createNavigation();

        int searchWidth = Math.max(120, Math.min(420, width - 210));
        int searchX = 10;
        searchBox = addRenderableWidget(new EditBox(
                font,
                searchX,
                SEARCH_Y,
                searchWidth,
                20,
                Component.literal(translated("screen.chisetweaks.help.search", "Search features"))));
        searchBox.setHint(Component.literal(translated("screen.chisetweaks.help.search", "機能を検索...")));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            scrollOffset = 0;
        });

        int languageWidth = 72;
        int languageGap = 4;
        int languageX = Math.max(searchX + searchWidth + 8, width - (languageWidth * 2 + languageGap + 10));
        japaneseButton = addRenderableWidget(Button.builder(
                Component.literal("日本語"),
                ignored -> selectLanguage(FeatureHelpDisplayLanguage.JAPANESE))
                .bounds(languageX, SEARCH_Y, languageWidth, 20)
                .build());
        englishButton = addRenderableWidget(Button.builder(
                Component.literal("English"),
                ignored -> selectLanguage(FeatureHelpDisplayLanguage.ENGLISH))
                .bounds(languageX + languageWidth + languageGap, SEARCH_Y, languageWidth, 20)
                .build());

        backButton = addRenderableWidget(Button.builder(
                Component.literal(translated("screen.chisetweaks.help.back", "Back to settings")),
                ignored -> onClose())
                .bounds(width / 2 - 50, height - 27, 100, 20)
                .build());
        refreshLanguageControls();
    }

    private void createNavigation() {
        int gap = 4;
        int available = Math.max(300, width - 20 - gap * 4);
        int buttonWidth = Math.max(56, Math.min(110, available / 5));
        int totalWidth = buttonWidth * 5 + gap * 4;
        int x = Math.max(10, (width - totalWidth) / 2);

        for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values()) {
            Button button = addRenderableWidget(Button.builder(
                    Component.literal(section.getDisplayName()),
                    ignored -> navigate(section))
                    .bounds(x, NAV_Y, buttonWidth, 20)
                    .build());
            button.active = section != ChiseTweaksUiSection.HELP;
            x += buttonWidth + gap;
        }
    }

    private void navigate(ChiseTweaksUiSection section) {
        if (minecraft == null || section == null || section == ChiseTweaksUiSection.HELP) return;
        minecraft.setScreen(new ChiseTweaksConfigScreen(section));
    }

    private void selectLanguage(FeatureHelpDisplayLanguage language) {
        if (language == null || language == displayLanguage) return;
        displayLanguage = language;
        scrollOffset = 0;
        refreshLanguageControls();
        if (searchBox != null) {
            searchBox.setHint(Component.literal(translated(
                    "screen.chisetweaks.help.search", "機能を検索...")));
        }
    }

    private void refreshLanguageControls() {
        if (backButton != null) {
            backButton.setMessage(Component.literal(translated(
                    "screen.chisetweaks.help.back", "Back to settings")));
        }
        if (japaneseButton != null) japaneseButton.active = displayLanguage != FeatureHelpDisplayLanguage.JAPANESE;
        if (englishButton != null) englishButton.active = displayLanguage != FeatureHelpDisplayLanguage.ENGLISH;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Mth.clamp(scrollOffset + (int) (-verticalAmount * 28), 0, maxScroll);
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        int panelX = Math.max(12, (width - Math.min(780, width - 24)) / 2);
        int panelWidth = Math.min(780, width - 24);
        extractor.fill(panelX, TOP - 5, panelX + panelWidth, height - BOTTOM, 0xD0181818);
        extractor.fill(panelX, TOP - 5, panelX + panelWidth, TOP - 4, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(
                font,
                Component.literal(translated("screen.chisetweaks.help.title", "ChiseTweaks Feature Guide")),
                width / 2,
                8,
                0xFFFFFFFF);
        extractor.centeredText(
                font,
                Component.literal(translated(
                        "screen.chisetweaks.help.subtitle",
                        "Browse Chise capabilities by the problem they solve.")),
                width / 2,
                18,
                0xFFB8B8B8);

        int panelWidth = Math.min(780, width - 24);
        int panelX = Math.max(12, (width - panelWidth) / 2);
        int contentWidth = panelWidth - 20;
        int y = TOP - scrollOffset;
        int contentHeight = 0;

        for (FeatureHelpEntry entry : filteredEntries()) {
            Card card = card(entry, contentWidth - CARD_PADDING * 2);
            int cardHeight = card.height();
            if (y + cardHeight >= TOP && y <= height - BOTTOM) {
                renderCard(extractor, card, panelX + 10, y, contentWidth, cardHeight);
            }
            y += cardHeight + CARD_GAP;
            contentHeight += cardHeight + CARD_GAP;
        }
        maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    private List<FeatureHelpEntry> filteredEntries() {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return FeatureHelpCatalog.entries();

        ArrayList<FeatureHelpEntry> result = new ArrayList<>();
        for (FeatureHelpEntry entry : FeatureHelpCatalog.entries()) {
            String searchable = String.join(" ",
                    translated(entry.nameKey(), entry.englishName()),
                    translated(entry.summaryKey(), ""),
                    translated(entry.usageKey(), ""),
                    translated(entry.requirementKey(), ""),
                    entry.englishName(),
                    entry.dependency()).toLowerCase(Locale.ROOT);
            if (searchable.contains(query)) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    private void renderCard(
            GuiGraphicsExtractor extractor,
            Card card,
            int x,
            int y,
            int width,
            int height) {
        int bottom = this.height - BOTTOM;
        int top = TOP;
        int visibleTop = Math.max(top, y);
        int visibleBottom = Math.min(bottom, y + height);
        if (visibleTop >= visibleBottom) {
            return;
        }
        extractor.fill(x, visibleTop, x + width, visibleBottom, 0xD0282828);
        extractor.fill(x, visibleTop, x + 3, visibleBottom, card.accentColor());

        int textX = x + CARD_PADDING;
        int lineY = y + CARD_PADDING;
        lineY = drawLines(extractor, card.titleLines(), textX, lineY, 0xFFFFFFFF, top, bottom);
        lineY = drawLines(extractor, card.badgeLines(), textX, lineY + 1, 0xFFFFD36A, top, bottom);
        lineY = drawLines(extractor, card.summaryLines(), textX, lineY + 3, 0xFFE4E4E4, top, bottom);
        lineY = drawLines(extractor, card.usageLines(), textX, lineY + 3, 0xFFB8DBFF, top, bottom);
        drawLines(extractor, card.requirementLines(), textX, lineY + 3, 0xFFB8B8B8, top, bottom);
    }

    private int drawLines(
            GuiGraphicsExtractor extractor,
            List<String> lines,
            int x,
            int y,
            int color,
            int top,
            int bottom) {
        for (String line : lines) {
            if (y >= top && y < bottom) {
                extractor.text(font, line, x, y, color);
            }
            y += LINE_HEIGHT;
        }
        return y;
    }

    private Card card(FeatureHelpEntry entry, int maxWidth) {
        String localizedName = translated(entry.nameKey(), entry.englishName());
        List<String> title = wrap(localizedName, maxWidth);
        List<String> badges = wrap(badges(entry), maxWidth);
        List<String> summary = wrap(
                translated(entry.summaryKey(), "No summary is available."), maxWidth);
        List<String> usage = wrap(
                translated("screen.chisetweaks.help.usage_prefix", "使い方: ")
                        + translated(entry.usageKey(), "See the feature settings."),
                maxWidth);
        List<String> requirement = wrap(
                translated("screen.chisetweaks.help.requirement_prefix", "条件: ")
                        + translated(entry.requirementKey(), dependencyFallback(entry)),
                maxWidth);
        int lineCount = title.size() + badges.size() + summary.size() + usage.size() + requirement.size();
        int height = CARD_PADDING * 2 + lineCount * LINE_HEIGHT + 10;
        return new Card(title, badges, summary, usage, requirement, height, accent(entry.level()));
    }

    private String badges(FeatureHelpEntry entry) {
        ArrayList<String> badges = new ArrayList<>();
        if (entry.defaultOff()) {
            badges.add(translated("screen.chisetweaks.help.badge.default_off", "[初期OFF]"));
        }
        badges.add(translated(
                "screen.chisetweaks.help.badge." + entry.level().name().toLowerCase(),
                "[" + entry.level().name() + "]"));
        if (!entry.dependency().isBlank()) {
            badges.add("[" + entry.dependency() + "]");
        }
        return String.join(" ", badges);
    }

    private String dependencyFallback(FeatureHelpEntry entry) {
        return entry.dependency().isBlank()
                ? translated("screen.chisetweaks.help.requirement.none", "追加MODは不要です。")
                : entry.dependency() + " "
                + translated("screen.chisetweaks.help.requirement.loaded", "が必要です。");
    }

    private int accent(FeatureHelpLevel level) {
        return switch (level) {
            case SAFETY -> 0xFF6EDC8C;
            case AUTOMATION -> 0xFFFFB86C;
            case DIAGNOSTIC -> 0xFF78B7FF;
            case INTEGRATION -> 0xFFC792EA;
            case ADVANCED -> 0xFFFF6B6B;
        };
    }

    private List<String> wrap(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        for (String paragraph : text.split("\\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int index = 0; index < paragraph.length(); index++) {
                char value = paragraph.charAt(index);
                String candidate = line.toString() + value;
                if (line.length() > 0 && font.width(candidate) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(value);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private String translated(String key, String fallback) {
        return FeatureHelpLanguageCatalog.text(displayLanguage, key, fallback);
    }

    private static FeatureHelpDisplayLanguage initialLanguage() {
        String probe = Component.translatable("screen.chisetweaks.help.language.probe").getString();
        return "ja".equalsIgnoreCase(probe) ? FeatureHelpDisplayLanguage.JAPANESE : FeatureHelpDisplayLanguage.ENGLISH;
    }

    private record Card(
            List<String> titleLines,
            List<String> badgeLines,
            List<String> summaryLines,
            List<String> usageLines,
            List<String> requirementLines,
            int height,
            int accentColor) {
    }
}
