package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.runtime.ClientCallbackCircuitBreaker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Builds and renders one immutable worksite HUD snapshot per scan. */
final class WorksiteHudPresenter {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(ChiseTweaksClient.MOD_ID, "worksite_visibility");
    private static final Component HUD_TITLE =
            Component.translatable("hud.chisetweaks.worksite_visibility.title");

    private final BooleanSupplier activeSupplier;
    private final Consumer<GuiGraphicsExtractor> guardedRender;
    private volatile HudSnapshot snapshot = HudSnapshot.EMPTY;

    WorksiteHudPresenter(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        this.guardedRender = this::renderSafely;
    }

    void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, HUD_ID, this::render);
    }

    void update(
            Minecraft client,
            LocalFeatureConfig config,
            WorksiteTargetInspection inspectedTarget,
            List<WorksiteVisibleTarget> targets) {
        ArrayList<Line> lines = new ArrayList<>();
        boolean hasTarget = inspectedTarget != null;
        int targetArgb = 0;
        if (hasTarget) {
            targetArgb = inspectedTarget.presentation().argb();
            lines.add(new Line(Component.translatable(
                    "hud.chisetweaks.worksite_visibility.target",
                    shortId(inspectedTarget.presentation().blockId())),
                    targetArgb));
            String details = inspectedTarget.presentation().compactDetails();
            if (!details.isEmpty()) lines.add(new Line(Component.literal(details), 0xFFE8E8E8));
        }

        int hudLimit = WorksiteVisibilityBudgetPolicy.clampHudResults(
                config.worksiteVisibilityMaxResults);
        BlockPos playerPosition = client.player.blockPosition();
        for (int index = 0; index < targets.size() && index < hudLimit; index++) {
            WorksiteVisibleTarget target = targets.get(index);
            BlockPos position = target.position();
            String relativePosition = signed(position.getX() - playerPosition.getX()) + ","
                    + signed(position.getY() - playerPosition.getY()) + ","
                    + signed(position.getZ() - playerPosition.getZ());
            lines.add(new Line(
                    Component.literal(shortId(target.presentation().blockId()) + "  " + relativePosition),
                    target.style().argb()));
        }

        List<Line> immutableLines = List.copyOf(lines);
        Font font = client.font;
        int width = font.width(HUD_TITLE);
        for (Line line : immutableLines) width = Math.max(width, font.width(line.text()));
        snapshot = new HudSnapshot(immutableLines, width, hasTarget, targetArgb);
    }

    void clear() {
        snapshot = HudSnapshot.EMPTY;
    }

    private void render(GuiGraphicsExtractor extractor, net.minecraft.client.DeltaTracker deltaTracker) {
        if (!activeSupplier.getAsBoolean()
                || ClientCallbackCircuitBreaker.isOpen(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_HUD_RENDER)) return;
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_HUD_RENDER,
                extractor,
                guardedRender);
    }

    private void renderSafely(GuiGraphicsExtractor extractor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || client.screen != null) return;
        HudSnapshot current = snapshot;
        List<Line> lines = current.lines();
        if (lines.isEmpty()) return;

        Font font = client.font;
        int x = 6;
        int y = 48;
        int lineHeight = font.lineHeight + 2;
        int height = lineHeight * (lines.size() + 1) + 4;
        extractor.fill(x - 3, y - 3, x + current.width() + 5, y + height, 0x98000000);
        extractor.text(font, HUD_TITLE, x, y, 0xFFFFFFFF, true);
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            extractor.text(font, line.text(), x, y + lineHeight * (index + 1), line.argb(), true);
        }

        if (current.hasTarget()) {
            int centerX = client.getWindow().getGuiScaledWidth() / 2;
            int centerY = client.getWindow().getGuiScaledHeight() / 2;
            int color = current.targetArgb();
            extractor.fill(centerX - 8, centerY - 8, centerX - 2, centerY - 7, color);
            extractor.fill(centerX + 2, centerY - 8, centerX + 8, centerY - 7, color);
            extractor.fill(centerX - 8, centerY + 7, centerX - 2, centerY + 8, color);
            extractor.fill(centerX + 2, centerY + 7, centerX + 8, centerY + 8, color);
        }
    }

    private static String shortId(String blockId) {
        int separator = blockId.indexOf(':');
        return separator >= 0 ? blockId.substring(separator + 1) : blockId;
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private record HudSnapshot(List<Line> lines, int width, boolean hasTarget, int targetArgb) {
        private static final HudSnapshot EMPTY = new HudSnapshot(List.of(), 0, false, 0);
    }

    private record Line(Component text, int argb) {}
}
