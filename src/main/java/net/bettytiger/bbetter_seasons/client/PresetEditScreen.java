package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.EventTypes;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.preset.CalendarPreset;
import net.bettytiger.bbetter_seasons.preset.PresetStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds or edits a calendar preset: a name plus a list of events, saved as a file. Changes are kept in
 * memory until Save, so Cancel discards them.
 */
public class PresetEditScreen extends Screen {

    private static final String KEY = "gui." + BettysBetterSeasonsMod.MODID + ".preset.";
    private static final int BUTTON_W = 98;
    private static final int GAP = 4;

    private final Screen parent;
    private final CalendarPreset preset;
    @Nullable
    private final Consumer<CalendarPreset> onSaved;
    private final List<CalendarEvent> working;
    private String pendingName;

    private EventList list;
    private Button editButton;
    private Button removeButton;

    /** @param onSaved called after the preset was written to disk, may be null */
    public PresetEditScreen(Screen parent, CalendarPreset preset, @Nullable Consumer<CalendarPreset> onSaved) {
        super(Component.translatable("screen." + BettysBetterSeasonsMod.MODID + ".preset_edit"));
        this.parent = parent;
        this.preset = preset;
        this.onSaved = onSaved;
        this.working = new ArrayList<>(preset.getEvents());
        this.pendingName = preset.getName();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int rowWidth = BUTTON_W * 3 + GAP * 2;
        int left = centerX - rowWidth / 2;

        EditBox nameBox = new EditBox(font, centerX - 100, 34, 200, 18, Component.translatable(KEY + "name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(pendingName);
        nameBox.setResponder(value -> pendingName = value);
        addRenderableWidget(nameBox);

        list = new EventList(this.minecraft, this.width, this.height, 62, this.height - 58);
        addRenderableWidget(list);

        int eventRow = this.height - 52;
        addRenderableWidget(new ShadowlessButton(left, eventRow, BUTTON_W, 20,
                Component.translatable(KEY + "add_event"), b -> addEvent()));
        editButton = addRenderableWidget(new ShadowlessButton(left + BUTTON_W + GAP, eventRow, BUTTON_W, 20,
                Component.translatable(KEY + "edit_event"), b -> editSelected()));
        removeButton = addRenderableWidget(new ShadowlessButton(left + (BUTTON_W + GAP) * 2, eventRow, BUTTON_W, 20,
                Component.translatable(KEY + "remove_event"), b -> removeSelected()));

        int actionRow = this.height - 28;
        int halfW = (rowWidth - GAP) / 2;
        addRenderableWidget(new ShadowlessButton(left, actionRow, halfW, 20,
                Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".save"), b -> save()));
        addRenderableWidget(new ShadowlessButton(left + halfW + GAP, actionRow, halfW, 20,
                CommonComponents.GUI_CANCEL, b -> onClose()));

        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = list.getSelected() != null;
        editButton.active = hasSelection;
        removeButton.active = hasSelection;
    }

    private void addEvent() {
        List<Season> seasons = SelectableSeasons.list(null);
        ResourceLocation firstSeason = seasons.isEmpty() ? null : seasons.get(0).id();
        this.minecraft.setScreen(new EventEditScreen(this, null, firstSeason, 1, working::add, null));
    }

    private void editSelected() {
        EventList.Entry entry = list.getSelected();
        if (entry == null) {
            return;
        }
        CalendarEvent original = entry.event;
        this.minecraft.setScreen(new EventEditScreen(this, original, original.getSeason(), original.getDay(),
                updated -> {
                    int index = working.indexOf(original);
                    if (index >= 0) {
                        working.set(index, updated);
                    }
                },
                working::remove));
    }

    private void removeSelected() {
        EventList.Entry entry = list.getSelected();
        if (entry != null) {
            working.remove(entry.event);
            rebuildWidgets();
        }
    }

    private void save() {
        String name = pendingName.trim();
        preset.setName(name.isEmpty() ? Component.translatable(KEY + "default_name").getString() : name);
        preset.setEvents(working);
        boolean saved = PresetStorage.save(preset);
        SystemToast.addOrUpdate(this.minecraft.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                Component.translatable(KEY + (saved ? "saved" : "save_failed")), Component.literal(preset.getName()));
        if (saved && onSaved != null) {
            onSaved.accept(preset);
        }
        this.minecraft.setScreen(parent);
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
        graphics.drawString(font, Component.translatable(KEY + "name"), this.width / 2 - 100, 24, 0xFFA0A0A0, false);
        if (working.isEmpty()) {
            Component empty = Component.translatable(KEY + "no_events");
            graphics.drawString(font, empty, this.width / 2 - font.width(empty) / 2, 80, 0xFFA0A0A0, false);
        }
    }

    /** The preset's events, ordered by season and day. */
    private final class EventList extends ObjectSelectionList<EventList.Entry> {

        EventList(Minecraft minecraft, int width, int height, int top, int bottom) {
            super(minecraft, width, height, top, bottom, 14);
            setRenderTopAndBottom(false);
            List<Season> order = SelectableSeasons.list(null);
            working.stream()
                    .sorted(Comparator.comparingInt((CalendarEvent event) -> seasonOrder(order, event.getSeason()))
                            .thenComparingInt(CalendarEvent::getDay))
                    .forEach(event -> addEntry(new Entry(event)));
        }

        private static int seasonOrder(List<Season> order, ResourceLocation season) {
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).id().equals(season)) {
                    return i;
                }
            }
            return order.size();
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
            final CalendarEvent event;

            Entry(CalendarEvent event) {
                this.event = event;
            }

            @Override
            public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                Font font = Minecraft.getInstance().font;
                Component when = Component.empty().append(SelectableSeasons.displayName(event.getSeason()))
                        .append(" " + event.getDay());
                Component type = Component.translatable(EventTypes.getOrDefault(event.getType()).translationKey());
                int titleX = left + 4 + Math.max(70, font.width(when) + 8);
                int titleWidth = left + width - font.width(type) - 10 - titleX;
                graphics.drawString(font, when, left + 4, top + 1, 0xFFA0A0A0, false);
                graphics.drawString(font, font.plainSubstrByWidth(event.getTitle(), Math.max(0, titleWidth)), titleX, top + 1, 0xFFFFFFFF, false);
                graphics.drawString(font, type, left + width - font.width(type) - 4, top + 1, 0xFF808080, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) {
                    return false;
                }
                EventList.this.setSelected(this);
                updateButtons();
                return true;
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.literal(event.getTitle());
            }
        }
    }
}
