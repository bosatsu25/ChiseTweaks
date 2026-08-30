package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.ChiseRuleModeSetting;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightModelReload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared editor shell for bounded user-owned ID lists.
 * Persistence and validation stay domain-specific; paging/render/footer lifecycle is shared.
 */
public final class ChiseListEditorScreen extends Screen {
    private static final int OUTER_MARGIN = 12;
    private static final int ROW_HEIGHT = 24;
    private static final int STANDARD_REMOVE_WIDTH = 68;

    public enum Target {
        BLOCK_FILTER,
        ENTITY_FILTER,
        ORE_COMPATIBILITY,
        LITEMATICA_PICK_REDIRECT,
        TWEAKERMORE_AUTO_PICK_GUARD,
        TWEAKEROO_TOOL_SWITCH_GUARD
    }

    private final Screen parent;
    private final Target target;
    private final ArrayList<Button> removeButtons = new ArrayList<>();
    private final SettingPersistenceCoordinator persistence = SettingPersistenceCoordinator.production();
    private final MasaIntegrationConfig masaConfig = MasaIntegrationConfig.getInstance();

    private EditBox firstBox;
    private EditBox secondBox;
    private Button modeButton;
    private Button styleButton;
    private Button addButton;
    private Button clearButton;
    private Button previousButton;
    private Button nextButton;
    private OreHighlightStyle selectedStyle = OreHighlightStyle.GENERIC;
    private ChiseOreCompatibilityLayout.Geometry oreLayout;
    private int panelX;
    private int panelWidth;
    private int listTop;
    private int pageSize;
    private int removeWidth = STANDARD_REMOVE_WIDTH;
    private boolean compactLayout;
    private int page;
    private String feedback = "";

    public ChiseListEditorScreen(Screen parent, Target target) {
        super(screenTitle(target));
        this.parent = parent;
        this.target = target == null ? Target.BLOCK_FILTER : target;
    }

    @Override
    protected void init() {
        super.init();
        if (isOre()) {
            initOreLayout();
        } else {
            initStandardLayout();
        }
        createRemoveButtons();
        createFooter();
        refreshControls();
    }

    private void initStandardLayout() {
        panelWidth = Math.min(760, Math.max(336, width - OUTER_MARGIN * 2));
        panelX = Math.max(OUTER_MARGIN, (width - panelWidth) / 2);
        listTop = isPickRedirect() ? 116 : 112;
        pageSize = Math.max(2, Math.min(10, (Math.max(260, height) - listTop - 54) / ROW_HEIGHT));
        removeWidth = STANDARD_REMOVE_WIDTH;
        compactLayout = false;

        if (!isPickRedirect()) {
            modeButton = addRenderableWidget(Button.builder(
                    modeMessage(), ignored -> cycleMode())
                    .bounds(panelX + 8, 42, Math.min(isSceneFilter() ? 220 : 240, panelWidth - 16), 20)
                    .build());
        }
        createStandardInputs();
    }

    private void createStandardInputs() {
        int addWidth = 74;
        if (isPickRedirect()) {
            int gap = 6;
            int usable = Math.max(160, panelWidth - addWidth - gap * 3 - 16);
            int fieldWidth = usable / 2;
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, fieldWidth, 20, Component.literal("Schematic block")));
            secondBox = addRenderableWidget(new EditBox(
                    font, panelX + 8 + fieldWidth + gap, 70, fieldWidth, 20,
                    Component.literal("Replacement block")));
            firstBox.setHint(Component.literal("minecraft:farmland"));
            secondBox.setHint(Component.literal("minecraft:dirt"));
            int addX = panelX + panelWidth - addWidth - 8;
            firstBox.setWidth(Math.max(70, addX - gap - firstBox.getX() - fieldWidth - gap));
            secondBox.setX(firstBox.getX() + firstBox.getWidth() + gap);
            secondBox.setWidth(Math.max(70, addX - gap - secondBox.getX()));
            addButton = addRenderableWidget(Button.builder(
                    Component.literal("追加"), ignored -> addEntry())
                    .bounds(addX, 70, addWidth, 20).build());
        } else {
            int inputWidth = Math.max(120, panelWidth - addWidth - 26);
            Component label = isSceneFilter()
                    ? Component.translatable("screen.chisetweaks.scene_filter.target_id")
                    : Component.literal("Target ID");
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, inputWidth, 20, label));
            firstBox.setHint(Component.literal(inputHint()));
            addButton = addRenderableWidget(Button.builder(
                    isSceneFilter()
                            ? Component.translatable("screen.chisetweaks.scene_filter.add")
                            : Component.literal("追加"),
                    ignored -> addEntry())
                    .bounds(panelX + 16 + inputWidth, 70, addWidth, 20).build());
        }
        firstBox.setResponder(ignored -> refreshControls());
        if (secondBox != null) secondBox.setResponder(ignored -> refreshControls());
    }

    private void initOreLayout() {
        oreLayout = ChiseOreCompatibilityLayout.calculate(width, height);
        panelX = oreLayout.panel().x();
        panelWidth = oreLayout.panel().width();
        listTop = oreLayout.listTop();
        pageSize = oreLayout.pageSize();
        removeWidth = oreLayout.removeWidth();
        compactLayout = oreLayout.compact();

        var idRect = oreLayout.idInput();
        firstBox = addRenderableWidget(new EditBox(
                font, idRect.x(), idRect.y(), idRect.width(), idRect.height(),
                Component.translatable("screen.chisetweaks.ore_compat.block_id")));
        firstBox.setHint(Component.literal("examplemod:copper_ore"));
        firstBox.setResponder(ignored -> refreshControls());

        var styleRect = oreLayout.style();
        styleButton = addRenderableWidget(Button.builder(styleMessage(), ignored -> {
            selectedStyle = selectedStyle.next();
            styleButton.setMessage(styleMessage());
        }).bounds(styleRect.x(), styleRect.y(), styleRect.width(), styleRect.height()).build());

        var addRect = oreLayout.add();
        addButton = addRenderableWidget(Button.builder(
                Component.translatable(compactLayout
                        ? "screen.chisetweaks.ore_compat.add"
                        : "screen.chisetweaks.ore_compat.add_update"),
                ignored -> addEntry())
                .bounds(addRect.x(), addRect.y(), addRect.width(), addRect.height())
                .build());
    }

    private void createRemoveButtons() {
        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            removeButtons.add(addRenderableWidget(Button.builder(
                    removeMessage(), ignored -> removeEntry(visibleSlot))
                    .bounds(panelX + panelWidth - removeWidth - 10,
                            listTop + slot * ROW_HEIGHT, removeWidth, 20)
                    .build()));
        }
    }

    private void createFooter() {
        if (isOre()) {
            previousButton = footerButton(oreLayout.previous(), previousMessage(), -1);
            nextButton = footerButton(oreLayout.next(), nextMessage(), 1);
            var clear = oreLayout.clear();
            clearButton = addRenderableWidget(Button.builder(clearMessage(), ignored -> clearEntries())
                    .bounds(clear.x(), clear.y(), clear.width(), clear.height()).build());
            var back = oreLayout.back();
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.chisetweaks.common.back"), ignored -> onClose())
                    .bounds(back.x(), back.y(), back.width(), back.height()).build());
            return;
        }

        int footerY = height - 28;
        previousButton = addRenderableWidget(Button.builder(previousMessage(), ignored -> movePage(-1))
                .bounds(panelX + 8, footerY, 58, 20).build());
        nextButton = addRenderableWidget(Button.builder(nextMessage(), ignored -> movePage(1))
                .bounds(panelX + 70, footerY, 58, 20).build());
        clearButton = addRenderableWidget(Button.builder(clearMessage(), ignored -> clearEntries())
                .bounds(panelX + 132, footerY, isSceneFilter() ? 104 : 110, 20).build());
        addRenderableWidget(Button.builder(backMessage(), ignored -> onClose())
                .bounds(panelX + panelWidth - 88, footerY, 80, 20).build());
    }

    private Button footerButton(
            ChiseOreCompatibilityLayout.Rect rect,
            Component message,
            int delta) {
        return addRenderableWidget(Button.builder(message, ignored -> movePage(delta))
                .bounds(rect.x(), rect.y(), rect.width(), rect.height()).build());
    }

    private void cycleMode() {
        if (isSceneFilter()) {
            cycleSceneMode();
        } else if (isMasaGuard()) {
            cycleMasaMode();
        }
    }

    private void cycleSceneMode() {
        ChiseRuleModeSetting setting = sceneModeSetting();
        ChiseRuleMode previous = setting.getValue();
        ChiseRuleMode next = previous == ChiseRuleMode.NONE
                ? ChiseRuleMode.BLACKLIST
                : previous == ChiseRuleMode.BLACKLIST ? ChiseRuleMode.WHITELIST : ChiseRuleMode.NONE;
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

    private void cycleMasaMode() {
        int previous = masaMode();
        int next = previous == MasaIdListPolicy.NONE
                ? MasaIdListPolicy.BLACKLIST
                : previous == MasaIdListPolicy.BLACKLIST
                        ? MasaIdListPolicy.WHITELIST
                        : MasaIdListPolicy.NONE;
        setMasaMode(next);
        if (!saveMasa()) {
            setMasaMode(previous);
            feedback = "保存に失敗しました";
        } else {
            page = 0;
            feedback = "";
        }
        refreshControls();
    }

    private void addEntry() {
        if (isOre()) {
            addOreEntry();
        } else if (isSceneFilter()) {
            addSceneEntry();
        } else {
            addMasaEntry();
        }
    }

    private void addSceneEntry() {
        ChiseStringListSetting setting = activeSceneListSetting();
        if (setting == null || firstBox == null) return;
        String raw = normalize(firstBox.getValue());
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            feedback = text("screen.chisetweaks.scene_filter.feedback.invalid_id");
            refreshControls();
            return;
        }
        if (!isRegisteredSceneTarget(id)) {
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
        firstBox.setValue("");
        page = Math.max(0, (sanitized.size() - 1) / pageSize);
        feedback = text("screen.chisetweaks.scene_filter.feedback.added");
        refreshControls();
    }

    private void addMasaEntry() {
        if (firstBox == null) return;
        String first = normalize(firstBox.getValue());
        if (!validRegisteredMasaId(first, target == Target.TWEAKERMORE_AUTO_PICK_GUARD)) {
            feedback = "登録済みIDを入力してください";
            refreshControls();
            return;
        }
        String entry = first;
        if (isPickRedirect()) {
            String second = secondBox == null ? "" : normalize(secondBox.getValue());
            if (!validRegisteredMasaId(second, false)) {
                feedback = "代替先Block IDが正しくありません";
                refreshControls();
                return;
            }
            entry = first + "," + second;
        }
        List<String> previous = List.copyOf(currentTextEntries());
        if (previous.contains(entry)) {
            feedback = "既に登録されています";
            refreshControls();
            return;
        }
        if (previous.size() >= MasaIntegrationConfig.MAX_LIST_ENTRIES) {
            feedback = "登録上限に達しています";
            refreshControls();
            return;
        }
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(entry);
        setMasaEntries(updated);
        if (!saveMasa()) {
            setMasaEntries(previous);
            feedback = "保存に失敗しました";
            refreshControls();
            return;
        }
        firstBox.setValue("");
        if (secondBox != null) secondBox.setValue("");
        page = Math.max(0, (updated.size() - 1) / pageSize);
        feedback = "追加しました";
        refreshControls();
    }

    private void addOreEntry() {
        if (firstBox == null) return;
        String raw = normalize(firstBox.getValue());
        Identifier id = Identifier.tryParse(raw);
        if (id == null || "minecraft".equals(id.getNamespace())) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.invalid");
            refreshControls();
            return;
        }
        if (!isRegisteredBlock(id)) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.unregistered");
            refreshControls();
            return;
        }
        if (!OreHighlightCompatibilityConfig.put(id.toString(), selectedStyle)) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.save_failed");
            refreshControls();
            return;
        }
        OreHighlightModelReload.request();
        firstBox.setValue("");
        page = Math.max(0, (entryCount() - 1) / pageSize);
        feedback = text("screen.chisetweaks.ore_compat.feedback.saved");
        refreshControls();
    }

    private void removeEntry(int visibleSlot) {
        int index = page * pageSize + visibleSlot;
        if (index < 0 || index >= entryCount()) return;
        if (isOre()) {
            removeOreEntry(index);
        } else if (isSceneFilter()) {
            removeSceneEntry(index);
        } else {
            removeMasaEntry(index);
        }
        clampPage();
        refreshControls();
    }

    private void removeSceneEntry(int index) {
        ChiseStringListSetting setting = activeSceneListSetting();
        if (setting == null) return;
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
    }

    private void removeMasaEntry(int index) {
        List<String> previous = List.copyOf(currentTextEntries());
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.remove(index);
        setMasaEntries(updated);
        if (!saveMasa()) {
            setMasaEntries(previous);
            feedback = "保存に失敗しました";
        } else {
            feedback = "削除しました";
        }
    }

    private void removeOreEntry(int index) {
        List<OreHighlightCompatibilityConfig.Entry> entries = oreEntries();
        if (OreHighlightCompatibilityConfig.remove(entries.get(index).blockId())) {
            OreHighlightModelReload.request();
            feedback = text("screen.chisetweaks.ore_compat.feedback.removed");
        } else {
            feedback = text("screen.chisetweaks.ore_compat.feedback.remove_failed");
        }
    }

    private void clearEntries() {
        if (entryCount() == 0) return;
        if (isOre()) {
            clearOreEntries();
        } else if (isSceneFilter()) {
            clearSceneEntries();
        } else {
            clearMasaEntries();
        }
        refreshControls();
    }

    private void clearSceneEntries() {
        ChiseStringListSetting setting = activeSceneListSetting();
        if (setting == null) return;
        List<String> previous = setting.getStrings();
        if (!setting.setStrings(List.of())) return;
        if (!persist(setting.persistence())) {
            setting.setStrings(previous);
            feedback = text("screen.chisetweaks.scene_filter.feedback.save_failed");
        } else {
            page = 0;
            feedback = text("screen.chisetweaks.scene_filter.feedback.cleared");
        }
    }

    private void clearMasaEntries() {
        List<String> previous = List.copyOf(currentTextEntries());
        setMasaEntries(List.of());
        if (!saveMasa()) {
            setMasaEntries(previous);
            feedback = "保存に失敗しました";
        } else {
            page = 0;
            feedback = "リストを消去しました";
        }
    }

    private void clearOreEntries() {
        if (!OreHighlightCompatibilityConfig.clear()) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.clear_failed");
            return;
        }
        OreHighlightModelReload.request();
        page = 0;
        feedback = text("screen.chisetweaks.ore_compat.feedback.cleared");
    }

    private boolean persist(SettingPersistence domain) {
        return persistence.save(Set.of(domain)).successful();
    }

    private boolean saveMasa() {
        masaConfig.sanitize();
        return masaConfig.save();
    }

    private int masaMode() {
        return target == Target.TWEAKERMORE_AUTO_PICK_GUARD
                ? MasaIdListPolicy.clampMode(masaConfig.tweakermoreAutoPickListMode)
                : target == Target.TWEAKEROO_TOOL_SWITCH_GUARD
                        ? MasaIdListPolicy.clampMode(masaConfig.tweakerooToolSwitchListMode)
                        : MasaIdListPolicy.NONE;
    }

    private void setMasaMode(int value) {
        int normalized = MasaIdListPolicy.clampMode(value);
        if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
            masaConfig.tweakermoreAutoPickListMode = normalized;
        } else if (target == Target.TWEAKEROO_TOOL_SWITCH_GUARD) {
            masaConfig.tweakerooToolSwitchListMode = normalized;
        }
    }

    private ChiseRuleModeSetting sceneModeSetting() {
        return target == Target.BLOCK_FILTER
                ? BuilderFocusConfig.BLOCK_RULE_MODE
                : BuilderFocusConfig.ENTITY_RULE_MODE;
    }

    private ChiseStringListSetting activeSceneListSetting() {
        ChiseRuleMode mode = sceneModeSetting().getValue();
        if (mode == ChiseRuleMode.NONE) return null;
        if (mode == ChiseRuleMode.WHITELIST) {
            return target == Target.BLOCK_FILTER
                    ? BuilderFocusConfig.BLOCK_WHITELIST
                    : BuilderFocusConfig.ENTITY_WHITELIST;
        }
        return target == Target.BLOCK_FILTER
                ? BuilderFocusConfig.BLOCK_BLACKLIST
                : BuilderFocusConfig.ENTITY_BLACKLIST;
    }

    private List<String> currentTextEntries() {
        if (isSceneFilter()) {
            ChiseStringListSetting setting = activeSceneListSetting();
            return setting == null ? List.of() : setting.getStrings();
        }
        if (isPickRedirect()) return masaConfig.pickRedirectMap;
        if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
            return masaMode() == MasaIdListPolicy.WHITELIST
                    ? masaConfig.tweakermoreAutoPickWhitelist
                    : masaMode() == MasaIdListPolicy.BLACKLIST
                            ? masaConfig.tweakermoreAutoPickBlacklist
                            : List.of();
        }
        if (target == Target.TWEAKEROO_TOOL_SWITCH_GUARD) {
            return masaMode() == MasaIdListPolicy.WHITELIST
                    ? masaConfig.tweakerooToolSwitchWhitelist
                    : masaMode() == MasaIdListPolicy.BLACKLIST
                            ? masaConfig.tweakerooToolSwitchBlacklist
                            : List.of();
        }
        return List.of();
    }

    private void setMasaEntries(List<String> values) {
        ArrayList<String> copy = new ArrayList<>(values == null ? List.of() : values);
        if (isPickRedirect()) {
            masaConfig.pickRedirectMap = copy;
        } else if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
            if (masaMode() == MasaIdListPolicy.WHITELIST) masaConfig.tweakermoreAutoPickWhitelist = copy;
            else if (masaMode() == MasaIdListPolicy.BLACKLIST) masaConfig.tweakermoreAutoPickBlacklist = copy;
        } else if (target == Target.TWEAKEROO_TOOL_SWITCH_GUARD) {
            if (masaMode() == MasaIdListPolicy.WHITELIST) masaConfig.tweakerooToolSwitchWhitelist = copy;
            else if (masaMode() == MasaIdListPolicy.BLACKLIST) masaConfig.tweakerooToolSwitchBlacklist = copy;
        }
    }

    private boolean isRegisteredSceneTarget(Identifier id) {
        if (target == Target.BLOCK_FILTER) return isRegisteredBlock(id);
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
        }
        return false;
    }

    private static boolean validRegisteredMasaId(String raw, boolean itemTarget) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return false;
        if (!itemTarget) return isRegisteredBlock(id);
        for (Item item : BuiltInRegistries.ITEM) {
            if (id.equals(BuiltInRegistries.ITEM.getKey(item))) return true;
        }
        return false;
    }

    private static boolean isRegisteredBlock(Identifier id) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) return true;
        }
        return false;
    }

    private void movePage(int delta) {
        page += delta;
        clampPage();
        refreshControls();
    }

    private void clampPage() {
        int size = entryCount();
        int maxPage = size == 0 ? 0 : (size - 1) / pageSize;
        page = Mth.clamp(page, 0, maxPage);
    }

    private void refreshControls() {
        clampPage();
        if (modeButton != null) modeButton.setMessage(modeMessage());
        if (styleButton != null) styleButton.setMessage(styleMessage());

        boolean editable = isEditable();
        if (firstBox != null) firstBox.active = editable;
        if (secondBox != null) secondBox.active = editable;
        if (addButton != null) addButton.active = canSubmitCurrentEntry(editable);
        if (clearButton != null) clearButton.active = editable && entryCount() != 0;

        int maxPage = entryCount() == 0 ? 0 : (entryCount() - 1) / pageSize;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < maxPage;
        int first = page * pageSize;
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.visible = editable && first + slot < entryCount();
            button.active = button.visible;
        }
    }

    private boolean canSubmitCurrentEntry(boolean editable) {
        if (!editable || firstBox == null) return false;
        String first = normalize(firstBox.getValue());
        if (isOre()) return canSubmitOreEntry(first, oreEntries());
        if (first.isEmpty()) return false;
        if (secondBox != null && normalize(secondBox.getValue()).isEmpty()) return false;
        int limit = isSceneFilter() ? ConfigListPolicy.MAX_ENTRIES : MasaIntegrationConfig.MAX_LIST_ENTRIES;
        return entryCount() < limit;
    }

    static boolean canSubmitOreEntry(
            String rawValue,
            List<OreHighlightCompatibilityConfig.Entry> entries) {
        if (rawValue == null || rawValue.isBlank()) return false;
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (entries != null) {
            for (OreHighlightCompatibilityConfig.Entry entry : entries) {
                if (entry != null && normalized.equals(entry.blockId())) return true;
            }
        }
        return (entries == null ? 0 : entries.size()) < OreHighlightCompatibilityConfig.MAX_ENTRIES;
    }

    private boolean isEditable() {
        if (isSceneFilter()) return sceneModeSetting().getValue() != ChiseRuleMode.NONE;
        if (isMasaGuard()) return masaMode() != MasaIdListPolicy.NONE;
        return true;
    }

    private int entryCount() {
        return isOre() ? oreEntries().size() : currentTextEntries().size();
    }

    private String entryText(int index) {
        if (isOre()) {
            OreHighlightCompatibilityConfig.Entry entry = oreEntries().get(index);
            return entry.blockId() + "  →  " + entry.style().key();
        }
        return currentTextEntries().get(index);
    }

    private Component modeMessage() {
        if (isSceneFilter()) {
            Component targetName = Component.translatable(target == Target.BLOCK_FILTER
                    ? "screen.chisetweaks.scene_filter.target.blocks"
                    : "screen.chisetweaks.scene_filter.target.entities");
            ChiseRuleMode mode = sceneModeSetting().getValue();
            String modeKey = mode == ChiseRuleMode.NONE
                    ? "screen.chisetweaks.scene_filter.mode.disabled"
                    : mode == ChiseRuleMode.WHITELIST
                            ? "screen.chisetweaks.scene_filter.mode.allow"
                            : "screen.chisetweaks.scene_filter.mode.hide";
            return Component.translatable(
                    "screen.chisetweaks.scene_filter.mode.label",
                    targetName,
                    Component.translatable(modeKey));
        }
        String modeName = switch (masaMode()) {
            case MasaIdListPolicy.WHITELIST -> "ALLOW / Whitelist";
            case MasaIdListPolicy.BLACKLIST -> "DENY / Blacklist";
            default -> "Disabled";
        };
        return Component.literal("Guard mode: " + modeName);
    }

    private String subtitleText() {
        if (isSceneFilter()) {
            ChiseRuleMode mode = sceneModeSetting().getValue();
            if (mode == ChiseRuleMode.NONE) {
                return text("screen.chisetweaks.scene_filter.mode.description.disabled");
            }
            return text(mode == ChiseRuleMode.WHITELIST
                    ? "screen.chisetweaks.scene_filter.mode.description.allow"
                    : "screen.chisetweaks.scene_filter.mode.description.hide");
        }
        if (isOre()) return text("screen.chisetweaks.ore_compat.subtitle");
        if (isMasaGuard()) return "外部MODの操作をChise policyで許可/拒否します。";
        return "";
    }

    private Component displayTitle() {
        if (isSceneFilter()) return Component.translatable("screen.chisetweaks.scene_filter.title");
        if (isOre()) return Component.translatable("screen.chisetweaks.ore_compat.title");
        return Component.literal(switch (target) {
            case LITEMATICA_PICK_REDIRECT -> "Litematica Pick Redirect";
            case TWEAKERMORE_AUTO_PICK_GUARD -> "TweakerMore Auto Pick Guard";
            case TWEAKEROO_TOOL_SWITCH_GUARD -> "Tweakeroo Tool Switch Guard";
            default -> "ChiseTweaks List Editor";
        });
    }

    private String inputHint() {
        return switch (target) {
            case BLOCK_FILTER -> "minecraft:stone";
            case ENTITY_FILTER -> "minecraft:item";
            case TWEAKERMORE_AUTO_PICK_GUARD -> "minecraft:golden_carrot";
            case TWEAKEROO_TOOL_SWITCH_GUARD -> "minecraft:glass";
            default -> "minecraft:stone";
        };
    }

    private Component removeMessage() {
        if (isOre()) return Component.translatable("screen.chisetweaks.ore_compat.remove");
        if (isSceneFilter()) return Component.translatable("screen.chisetweaks.scene_filter.remove");
        return Component.literal("削除");
    }

    private Component previousMessage() {
        if (isOre()) {
            return compactLayout
                    ? Component.literal("‹")
                    : Component.translatable("screen.chisetweaks.ore_compat.previous");
        }
        if (isSceneFilter()) return Component.translatable("screen.chisetweaks.scene_filter.previous");
        return Component.literal("前");
    }

    private Component nextMessage() {
        if (isOre()) {
            return compactLayout
                    ? Component.literal("›")
                    : Component.translatable("screen.chisetweaks.ore_compat.next");
        }
        if (isSceneFilter()) return Component.translatable("screen.chisetweaks.scene_filter.next");
        return Component.literal("次");
    }

    private Component clearMessage() {
        if (isOre()) {
            return Component.translatable(compactLayout
                    ? "screen.chisetweaks.ore_compat.clear"
                    : "screen.chisetweaks.ore_compat.clear_overrides");
        }
        if (isSceneFilter()) return Component.translatable("screen.chisetweaks.scene_filter.clear");
        return Component.literal("リストを消去");
    }

    private Component backMessage() {
        return isSceneFilter()
                ? Component.translatable("screen.chisetweaks.common.back")
                : Component.literal("戻る");
    }

    private Component styleMessage() {
        return Component.translatable("screen.chisetweaks.ore_compat.style", selectedStyle.key());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        if (isOre()) {
            extractor.fill(panelX, 34, panelX + panelWidth, Math.max(35, height - 34), 0xC8121212);
            int first = page * pageSize;
            for (int slot = 0; slot < pageSize && first + slot < entryCount(); slot++) {
                int y = listTop + slot * ROW_HEIGHT;
                extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            }
            return;
        }
        int panelBottom = Math.max(listTop + ROW_HEIGHT * pageSize + 4, height - 36);
        extractor.fill(panelX, 34, panelX + panelWidth, panelBottom, 0xD0181818);
        extractor.fill(panelX, 34, panelX + panelWidth, 35, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, displayTitle(), width / 2, 10, 0xFFFFFFFF);
        String subtitle = subtitleText();
        if (!subtitle.isEmpty()) {
            extractor.centeredText(font, Component.literal(subtitle), width / 2, 24, 0xFFB8B8B8);
        }

        int first = page * pageSize;
        int textRight = panelX + panelWidth - removeWidth - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entryCount()) break;
            int y = listTop + slot * ROW_HEIGHT;
            if (!isOre()) {
                extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            }
            extractor.text(
                    font,
                    ellipsize(entryText(index), Math.max(isOre() ? 40 : 80, textRight - panelX - 18)),
                    panelX + 14,
                    y + 5,
                    0xFFFFFFFF);
        }

        int count = entryCount();
        int maxPage = count == 0 ? 0 : (count - 1) / pageSize;
        extractor.centeredText(
                font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + count + ")"),
                width / 2,
                Math.max(0, height - 42),
                0xFFAAAAAA);
        if (!feedback.isEmpty()) {
            extractor.centeredText(
                    font,
                    Component.literal(feedback),
                    width / 2,
                    isOre() && compactLayout ? 104 : 96,
                    0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) {
            return value == null ? "" : value;
        }
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, end) + "…";
    }

    private static Component screenTitle(Target target) {
        if (target == Target.ORE_COMPATIBILITY) {
            return Component.translatable("screen.chisetweaks.ore_compat.title");
        }
        if (target == Target.BLOCK_FILTER || target == Target.ENTITY_FILTER || target == null) {
            return Component.translatable("screen.chisetweaks.scene_filter.title");
        }
        return Component.literal("Masa Integration");
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static List<OreHighlightCompatibilityConfig.Entry> oreEntries() {
        return OreHighlightCompatibilityConfig.entries();
    }

    private boolean isSceneFilter() {
        return target == Target.BLOCK_FILTER || target == Target.ENTITY_FILTER;
    }

    private boolean isOre() {
        return target == Target.ORE_COMPATIBILITY;
    }

    private boolean isPickRedirect() {
        return target == Target.LITEMATICA_PICK_REDIRECT;
    }

    private boolean isMasaGuard() {
        return target == Target.TWEAKERMORE_AUTO_PICK_GUARD
                || target == Target.TWEAKEROO_TOOL_SWITCH_GUARD;
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
