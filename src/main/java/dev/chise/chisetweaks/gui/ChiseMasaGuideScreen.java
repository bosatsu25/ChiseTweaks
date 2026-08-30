package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.integration.masa.MasaModAvailability;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public final class ChiseMasaGuideScreen extends Screen {
    private static final int MARGIN = 14;
    private static final int CARD_GAP = 8;
    private final Screen parent;
    private List<MasaGuideCatalog.Entry> entries = List.of();
    private int panelX;
    private int panelWidth;
    private int panelTop;
    private int panelBottom;
    private int scroll;
    private int maxScroll;

    public ChiseMasaGuideScreen(Screen parent) {
        super(Component.literal("Masa Guide"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        entries = MasaGuideCatalog.entries(MasaModAvailability.snapshot());
        panelWidth = Math.min(820, Math.max(300, width - MARGIN * 2));
        panelX = Math.max(0, (width - panelWidth) / 2);
        panelTop = 42;
        panelBottom = Math.max(panelTop + 1, height - 38);
        addRenderableWidget(Button.builder(
                Component.literal("戻る"),
                ignored -> onClose())
                .bounds(panelX + panelWidth - 88, height - 28, 80, 20)
                .build());
        recalculateScroll();
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {
        if (mouseX >= panelX && mouseX <= panelX + panelWidth
                && mouseY >= panelTop && mouseY <= panelBottom) {
            scroll = Mth.clamp(scroll + (int) (-verticalAmount * 32), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        extractor.fill(panelX, panelTop, panelX + panelWidth, panelBottom, 0xD0181818);
        extractor.fill(panelX, panelTop, panelX + panelWidth, panelTop + 1, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, Component.literal("Masa Ecosystem Guide"), width / 2, 12, 0xFFFFFFFF);
        extractor.centeredText(
                font,
                Component.literal("日本語で『何のMODか・どこを見るか』を確認できます。英語の機能名は検索用に残します。"),
                width / 2, 26, 0xFFB8B8B8);

        extractor.enableScissor(panelX, panelTop, panelX + panelWidth, panelBottom);
        try {
            int y = panelTop + 10 - scroll;
            int textWidth = Math.max(80, panelWidth - 32);
            for (MasaGuideCatalog.Entry entry : entries) {
                List<?> summary = font.split(Component.literal(entry.summary()), textWidth);
                List<?> directions = font.split(Component.literal(entry.directions()), textWidth);
                int cardHeight = 30 + (summary.size() + directions.size()) * (font.lineHeight + 2);
                extractor.fill(panelX + 8, y, panelX + panelWidth - 8, y + cardHeight, 0x88303030);
                int statusColor = entry.installed() ? 0xFF78E08F : 0xFFAAAAAA;
                extractor.text(
                        font,
                        entry.name() + (entry.installed() ? "  ✓ 導入済み" : "  — 未導入"),
                        panelX + 16, y + 7, statusColor);

                int lineY = y + 22;
                for (var line : font.split(Component.literal(entry.summary()), textWidth)) {
                    extractor.text(font, line, panelX + 16, lineY, 0xFFFFFFFF);
                    lineY += font.lineHeight + 2;
                }
                lineY += 2;
                for (var line : font.split(Component.literal(entry.directions()), textWidth)) {
                    extractor.text(font, line, panelX + 16, lineY, 0xFFB8B8B8);
                    lineY += font.lineHeight + 2;
                }
                y += cardHeight + CARD_GAP;
            }
        } finally {
            extractor.disableScissor();
        }
    }

    private void recalculateScroll() {
        int textWidth = Math.max(80, panelWidth - 32);
        int content = 10;
        for (MasaGuideCatalog.Entry entry : entries) {
            int summaryLines = font.split(Component.literal(entry.summary()), textWidth).size();
            int directionLines = font.split(Component.literal(entry.directions()), textWidth).size();
            content += 30 + (summaryLines + directionLines) * (font.lineHeight + 2) + CARD_GAP;
        }
        maxScroll = Math.max(0, content - Math.max(1, panelBottom - panelTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
