package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.preset.CalendarPreset;
import net.bettytiger.bbetter_seasons.preset.PresetSelection;
import net.bettytiger.bbetter_seasons.preset.PresetStorage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lists saved calendar presets from the Create World screen. Pick one to pre-fill the new world's
 * calendar, or create, edit and delete presets.
 */
public class PresetListScreen extends Screen {

    private static final String KEY = "gui." + BettysBetterSeasonsMod.MODID + ".preset.";
    private static final int BUTTON_W = 98;
    private static final int GAP = 4;

    private final Screen parent;
    /** Preset to highlight after returning from the editor (e.g. one that was just created). */
    @Nullable
    private String highlightFile;

    private PresetList list;
    private Button useButton;
    private Button editButton;
    private Button deleteButton;

    public PresetListScreen(Screen parent) {
        super(Component.translatable("screen." + BettysBetterSeasonsMod.MODID + ".presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<CalendarPreset> presets = PresetStorage.loadAll();
        PresetSelection.refresh(presets);

        list = new PresetList(this.minecraft, this.width, this.height, 32, this.height - 64, presets);
        addRenderableWidget(list);

        int rowWidth = BUTTON_W * 3 + GAP * 2;
        int left = this.width / 2 - rowWidth / 2;

        int chooseRow = this.height - 56;
        useButton = addRenderableWidget(new ShadowlessButton(left, chooseRow, BUTTON_W, 20,
                Component.translatable(KEY + "use"), b -> useSelected()));
        addRenderableWidget(new ShadowlessButton(left + BUTTON_W + GAP, chooseRow, BUTTON_W, 20,
                Component.translatable(KEY + "clear"), b -> {
                    PresetSelection.clear();
                    onClose();
                }));
        addRenderableWidget(new ShadowlessButton(left + (BUTTON_W + GAP) * 2, chooseRow, BUTTON_W, 20,
                CommonComponents.GUI_DONE, b -> onClose()));

        int manageRow = this.height - 32;
        int quarterW = (rowWidth - GAP * 3) / 4;
        addRenderableWidget(new ShadowlessButton(left, manageRow, quarterW, 20,
                Component.translatable(KEY + "new"), b -> openEditor(new CalendarPreset(
                        Component.translatable(KEY + "default_name").getString(), null, List.of()))));
        editButton = addRenderableWidget(new ShadowlessButton(left + quarterW + GAP, manageRow, quarterW, 20,
                Component.translatable(KEY + "edit"), b -> {
                    PresetList.Entry entry = list.getSelected();
                    if (entry != null) {
                        openEditor(entry.preset);
                    }
                }));
        deleteButton = addRenderableWidget(new ShadowlessButton(left + (quarterW + GAP) * 2, manageRow, quarterW, 20,
                Component.translatable(KEY + "delete"), b -> deleteSelected()));
        addRenderableWidget(new ShadowlessButton(left + (quarterW + GAP) * 3, manageRow, quarterW, 20,
                Component.translatable(KEY + "open_folder"), b -> Util.getPlatform().openFile(PresetStorage.directory().toFile())));

        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = list.getSelected() != null;
        useButton.active = hasSelection;
        editButton.active = hasSelection;
        deleteButton.active = hasSelection;
    }

    private void useSelected() {
        PresetList.Entry entry = list.getSelected();
        if (entry != null) {
            PresetSelection.select(entry.preset);
            onClose();
        }
    }

    private void openEditor(CalendarPreset preset) {
        this.minecraft.setScreen(new PresetEditScreen(this, preset, saved -> highlightFile = saved.getFileName()));
    }

    private void deleteSelected() {
        PresetList.Entry entry = list.getSelected();
        if (entry == null) {
            return;
        }
        CalendarPreset preset = entry.preset;
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                if (PresetSelection.isSelected(preset)) {
                    PresetSelection.clear();
                }
                PresetStorage.delete(preset);
            }
            this.minecraft.setScreen(this);
        }, Component.translatable(KEY + "delete_confirm.title", preset.getName()),
                Component.translatable(KEY + "delete_confirm.message")));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, this.title, this.width / 2 - font.width(this.title) / 2, 12, 0xFFFFFFFF, false);
        if (list.children().isEmpty()) {
            Component empty = Component.translatable(KEY + "empty");
            graphics.drawString(font, empty, this.width / 2 - font.width(empty) / 2, 50, 0xFFA0A0A0, false);
        }
    }

    /** Saved presets; the one chosen for this world is marked. */
    private final class PresetList extends ObjectSelectionList<PresetList.Entry> {

        PresetList(Minecraft minecraft, int width, int height, int top, int bottom, List<CalendarPreset> presets) {
            super(minecraft, width, height, top, bottom, 24);
            setRenderTopAndBottom(false);
            for (CalendarPreset preset : presets) {
                Entry entry = new Entry(preset);
                addEntry(entry);
                boolean highlight = highlightFile != null
                        ? highlightFile.equals(preset.getFileName())
                        : PresetSelection.isSelected(preset);
                if (highlight) {
                    setSelected(entry);
                }
            }
            highlightFile = null;
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 146;
        }

        final class Entry extends ObjectSelectionList.Entry<Entry> {
            final CalendarPreset preset;

            Entry(CalendarPreset preset) {
                this.preset = preset;
            }

            @Override
            public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                Font font = Minecraft.getInstance().font;
                graphics.drawString(font, font.plainSubstrByWidth(preset.getName(), width - 8), left + 4, top + 1, 0xFFFFFFFF, false);
                Component details = Component.translatable(KEY + "events", preset.getEvents().size());
                if (PresetSelection.isSelected(preset)) {
                    details = Component.empty().append(details).append("  ")
                            .append(Component.translatable(KEY + "chosen").withStyle(style -> style.withColor(0x7FD87F)));
                }
                graphics.drawString(font, details, left + 4, top + 11, 0xFFA0A0A0, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) {
                    return false;
                }
                PresetList.this.setSelected(this);
                updateButtons();
                return true;
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.literal(preset.getName());
            }
        }
    }
}
