package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.bettytiger.bbetter_seasons.network.EditEventPacket;
import net.bettytiger.bbetter_seasons.preset.CalendarPreset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Full-screen host for the {@link CalendarPanel}, opened by the handheld item, wall block, or API. */
public class CalendarScreen extends Screen {
    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 190;

    private CalendarPanel panel;

    public CalendarScreen() {
        super(Component.translatable("screen.bbetter_seasons.calendar"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(this.width - 40, MAX_WIDTH);
        int panelHeight = Math.min(this.height - 60, MAX_HEIGHT);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 10;
        panel = new CalendarPanel(panelX, panelY, panelWidth, panelHeight);
        addRenderableWidget(panel);

        // Editor controls for players who may edit events, plus "Save as Preset" for everyone.
        List<ShadowlessButton> row = new ArrayList<>();
        if (ClientPermissions.canCreate()) {
            row.add(rowButton("add_event", b -> openAdd()));
            row.add(rowButton("edit", b -> openEdit()));
            row.add(rowButton("delete", b -> deleteSelected()));
        }
        row.add(rowButton("preset.save_from_world", b -> saveAsPreset()));

        int buttonWidth = Math.min(96, (panelWidth - 4 * (row.size() - 1)) / row.size());
        int rowWidth = buttonWidth * row.size() + 4 * (row.size() - 1);
        int x = panelX + (panelWidth - rowWidth) / 2;
        for (ShadowlessButton button : row) {
            button.setX(x);
            button.setY(panelY + panelHeight + 4);
            button.setWidth(buttonWidth);
            addRenderableWidget(button);
            x += buttonWidth + 4;
        }
    }

    private static ShadowlessButton rowButton(String key, Button.OnPress onPress) {
        return new ShadowlessButton(0, 0, 0, 20, Component.translatable("gui." + BettysBetterSeasonsMod.MODID + "." + key), onPress);
    }

    /** Opens the preset editor pre-filled with this world's events, to reuse them in other worlds. */
    private void saveAsPreset() {
        List<CalendarEvent> events = new ArrayList<>();
        for (CalendarEvent event : ClientEvents.all()) {
            events.add(CalendarEvent.load(event.save()));
        }
        String name = Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".preset.default_name").getString();
        assert this.minecraft != null;
        this.minecraft.setScreen(new PresetEditScreen(this, new CalendarPreset(name, null, events), null));
    }

    private void openAdd() {
        int day = panel.getSelectedDay() > 0 ? panel.getSelectedDay() : 1;
        assert this.minecraft != null;
        this.minecraft.setScreen(new EventEditScreen(this, null, panel.getViewedSeasonId(), day));
    }

    private void openEdit() {
        CalendarEvent selected = panel.getSelectedEvent();
        if (selected != null && ClientPermissions.canModify(selected)) {
            assert this.minecraft != null;
            this.minecraft.setScreen(new EventEditScreen(this, selected, selected.getSeason(), selected.getDay()));
        }
    }

    private void deleteSelected() {
        CalendarEvent selected = panel.getSelectedEvent();
        if (selected != null && ClientPermissions.canModify(selected)) {
            BetterSeasonsNetwork.sendToServer(EditEventPacket.delete(selected.getId()));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Pauses singleplayer while the calendar is open, so players can set up their seasons in peace. */
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
