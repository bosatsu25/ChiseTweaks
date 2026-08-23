package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.ChiseRuleModeSetting;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChiseSceneFilterEditorScreen extends Screen {
    private static final int OUTER_MARGIN = 12;
    private static final int ROW_HEIGHT = 24;
    private static final int REMOVE_WIDTH = 68;

    public enum Target {
        BLOCKS,
        ENTITIES
    }

    private final Screen parent;
    private final Target target;
    private final ArrayList<Button> removeButtons = new ArrayList<>();
    private final SettingPersistenceCoordinator persistence = SettingPersistenceCoordinator.production();

    private EditBox idBox;
    private Button modeButton;
    private Button addButton;
    private Button clearButton;
    private Button previousButton;
    private Button nextButton;
    private int panelX;
    private int panelWidth;
    private int listTop;
    private int pageSize;
    private int page;
    private String feedback = "";

    public ChiseSceneFilterEditorScreen(Screen parent, Target target) {
        super(Component.translatable("screen.chisetweaks.scene_filter.title"));
        this.parent = parent;
        this.target = target == null ? Target.BLOCKS : target;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(760, Math.max(336, width - OUTER_MARGIN * 2));
        panelX = Math.max(OUTER_MARGIN, (width - panelWidth) / 2);
        listTop = 112;
        pageSize = Math.max(2, Math.min(10, (Math.max(260, height) - listTop - 54) / ROW_HEIGHT));

        modeButton = addRenderableWidget(Button.builder(
                modeMessage(), ignored -> cycleMode())
                .bounds(panelX + 8, 42, Math.min(220, panelWidth - 16), 20)
                .build());

        int addWidth = 74;
        int inputWidth = Math.max(120, panelWidth - addWidth - 26);
        idBox = addRenderableWidget(new EditBox(
                font,
                panelX + 8,
                70,
                inputWidth,
                20,
                Component.translatable("screen.chisetweaks.scene_filter.target_id")));
        idBox.setHint(Component.literal(target == Target.BLOCKS
                ? "minecraft:stone"
                : "minecraft:item"));
        idBox.setResponder(ignored -> refreshControls());

        addButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.scene_filter.add"), ignored -> addEntry())
                .bounds(panelX + 16 + inputWidth, 70, addWidth, 20)
                .build());

        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            Button remove = addRenderableWidget(Button.builder(
                    Component.translatable("screen.chisetweaks.scene_filter.remove"),
                    ignored -> removeEntry(visibleSlot))
                    .bounds(
                            panelX + panelWidth - REMOVE_WIDTH - 10,
                            listTop + slot * ROW_HEIGHT,
                            REMOVE_WIDTH,
                            20)
                    .build());
            removeButtons.add(remove);
        }

        int footerY = height - 28;
        previousButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.scene_filter.previous"), ignored -> movePage(-1))
                .bounds(panelX + 8, footerY, 58, 20).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.scene_filter.next"), ignored -> movePage(1))
                .bounds(panelX + 70, footerY, 58, 20).build());
        clearButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.scene_filter.clear"),
                ignored -> clearEntries())
                .bounds(panelX + 132, footerY, 104, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.common.back"), ignored -> onClose())
                .bounds(panelX + panelWidth - 88, footerY, 80, 20).build());

        refreshControls();
    }

    private void cycleMode() {
        ChiseRuleModeSetting setting = modeSetting();
        ChiseRuleMode previous = setting.getValue();
        ChiseRuleMode next = switch (previous) {
            case NONE -> ChiseRuleMode.BLACKLIST;
            case BLACKLIST -> ChiseRuleMode.WHITELIST;
            case WHITELIST -> ChiseRuleMode.NONE;
        };
        if (!setting.setValue(next)) return;
        if (!persist(setting.persistence())) {
            setting.setValue(previous);
            feedback = text("screen.chisetweaks.scene_filter.feedback.save_failed");
        } else {
            page = 0;
            feedback = "";
        }
        refreshControls();
    }

    private void addEntry() {
        ChiseStringListSetting setting = activeListSetting();
        if (setting == null || idBox == null) return;

        String raw = idBox.getValue() == null ? "" : idBox.getValue().trim().toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            feedback = text("screen.chisetweaks.scene_filter.feedback.invalid_id");
            refreshControls();
            return;
        }
        if (!isRegisteredTarget(id)) {
            feedback = text("screen.chisetweaks.scene_filter.feedback.unregistered");
            refreshControls();
            return;
        }

        String normalized = id.toString();
        if (setting.getStrings().contains(normalized)) {
            feedback = text("screen.chisetweaks.scene_filter.feedback.duplicate");
            refreshControls();
            return;
        }

        List<String> previous = setting.getStrings();
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(normalized);
        List<String> sanitized = ConfigListPolicy.sanitize(updated);
        if (!sanitized.contains(normalized)) {
            feedback = text("screen.chisetweaks.scene_filter.feedback.rejected");
            refreshControls();
            return;
        }

        if (!setting.setStrings(sanitized)) return;
        if (!persist(setting.persistence())) {
            setting.setStrings(previous);
            feedback = text("screen.chisetweaks.scene_filter.feedback.save_failed");
            refreshControls();
            return;
        }
        idBox.setValue("");
        page = Math.max(0, (sanitized.size() - 1) / pageSize);
        feedback = text("screen.chisetweaks.scene_filter.feedback.added");
        refreshControls();
    }

    private boolean isRegisteredTarget(Identifier id) {
        if (target == Target.BLOCKS) {
            for (Block block : BuiltInRegistries.BLOCK) {
                if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) return true;
            }
            return false;
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
        }
        return false;
    }

    private void removeEntry(int visibleSlot) {
        ChiseStringListSetting setting = activeListSetting();
        if (setting == null) return;
        int index = page * pageSize + visibleSlot;
        if (index < 0 || index >= setting.getStrings().size()) return;

        List<String> previous = setting.getStrings();
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.remove(index);
        if (!setting.setStrings(updated)) return;
        if (!persist(setting.persistence())) {
            setting.setStrings(previous);
            feedback = text("screen.chisetweaks.scene_filter.feedback.save_failed");
        } else {
            feedback = text("screen.chisetweaks.scene_filter.feedback.removed");
        }
        clampPage();
        refreshControls();
    }

    private void clearEntries() {
        ChiseStringListSetting setting = activeListSetting();
        if (setting == null || setting.getStrings().isEmpty()) return;
        List<String> previous = setting.getStrings();
        if (!setting.setStrings(List.of())) return;
        if (!persist(setting.persistence())) {
            setting.setStrings(previous);
            feedback = text("screen.chisetweaks.scene_filter.feedback.save_failed");
        } else {
            page = 0;
            feedback = text("screen.chisetweaks.scene_filter.feedback.cleared");
        }
        refreshControls();
    }

    private boolean persist(SettingPersistence domain) {
        return persistence.save(Set.of(domain)).successful();
    }

    private void movePage(int delta) {
        page += delta;
        clampPage();
        refreshControls();
    }

    private void clampPage() {
        int size = currentEntries().size();
        int maxPage = size == 0 ? 0 : (size - 1) / pageSize;
        page = Mth.clamp(page, 0, maxPage);
    }

    private void refreshControls() {
        if (modeButton == null) return;
        clampPage();
        modeButton.setMessage(modeMessage());

        ChiseRuleMode mode = modeSetting().getValue();
        List<String> entries = currentEntries();
        boolean editable = mode != ChiseRuleMode.NONE;
        if (idBox != null) idBox.active = editable;
        if (addButton != null) {
            String value = idBox == null || idBox.getValue() == null ? "" : idBox.getValue().trim();
            addButton.active = editable && !value.isEmpty() && entries.size() < ConfigListPolicy.MAX_ENTRIES;
        }
        if (clearButton != null) clearButton.active = editable && !entries.isEmpty();

        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < maxPage;

        int first = page * pageSize;
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.visible = editable && first + slot < entries.size();
            button.active = button.visible;
        }
    }

    private Component modeMessage() {
        Component targetName = Component.translatable(target == Target.BLOCKS
                ? "screen.chisetweaks.scene_filter.target.blocks"
                : "screen.chisetweaks.scene_filter.target.entities");
        Component modeName = Component.translatable(switch (modeSetting().getValue()) {
            case NONE -> "screen.chisetweaks.scene_filter.mode.disabled";
            case WHITELIST -> "screen.chisetweaks.scene_filter.mode.allow";
            case BLACKLIST -> "screen.chisetweaks.scene_filter.mode.hide";
        });
        return Component.translatable("screen.chisetweaks.scene_filter.mode.label", targetName, modeName);
    }

    private String modeDescription() {
        return text(switch (modeSetting().getValue()) {
            case NONE -> "screen.chisetweaks.scene_filter.mode.description.disabled";
            case WHITELIST -> "screen.chisetweaks.scene_filter.mode.description.allow";
            case BLACKLIST -> "screen.chisetweaks.scene_filter.mode.description.hide";
        });
    }

    private ChiseRuleModeSetting modeSetting() {
        return target == Target.BLOCKS
                ? BuilderFocusConfig.BLOCK_RULE_MODE
                : BuilderFocusConfig.ENTITY_RULE_MODE;
    }

    private ChiseStringListSetting activeListSetting() {
        return switch (modeSetting().getValue()) {
            case NONE -> null;
            case WHITELIST -> target == Target.BLOCKS
                    ? BuilderFocusConfig.BLOCK_WHITELIST
                    : BuilderFocusConfig.ENTITY_WHITELIST;
            case BLACKLIST -> target == Target.BLOCKS
                    ? BuilderFocusConfig.BLOCK_BLACKLIST
                    : BuilderFocusConfig.ENTITY_BLACKLIST;
        };
    }

    private List<String> currentEntries() {
        ChiseStringListSetting setting = activeListSetting();
        return setting == null ? List.of() : setting.getStrings();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        int panelBottom = Math.max(listTop + ROW_HEIGHT * pageSize + 4, height - 36);
        extractor.fill(panelX, 34, panelX + panelWidth, panelBottom, 0xD0181818);
        extractor.fill(panelX, 34, panelX + panelWidth, 35, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(
                font,
                Component.translatable("screen.chisetweaks.scene_filter.title"),
                width / 2,
                10,
                0xFFFFFFFF);
        extractor.centeredText(font, Component.literal(modeDescription()), width / 2, 24, 0xFFB8B8B8);

        List<String> entries = currentEntries();
        int first = page * pageSize;
        int textRight = panelX + panelWidth - REMOVE_WIDTH - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;
            String entry = entries.get(index);
            int y = listTop + slot * ROW_HEIGHT;
            extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            String shown = ellipsize(entry, Math.max(80, textRight - panelX - 18));
            extractor.text(font, shown, panelX + 14, y + 5, 0xFFFFFFFF);
        }

        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        extractor.centeredText(
                font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + entries.size() + ")"),
                width / 2,
                height - 42,
                0xFFAAAAAA);
        if (!feedback.isEmpty()) {
            extractor.centeredText(font, Component.literal(feedback), width / 2, 96, 0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) return value == null ? "" : value;
        String ellipsis = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return value.substring(0, end) + ellipsis;
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
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
