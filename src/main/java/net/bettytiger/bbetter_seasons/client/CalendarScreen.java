package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.bettytiger.bbetter_seasons.network.EditEventPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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

        // Editor controls: shown only to players who may edit events.
        if (ClientPermissions.canCreate()) {
            int buttonsY = panelY + panelHeight + 4;
            int buttonWidth = Math.min(96, (panelWidth - 8) / 3);
            addRenderableWidget(new ShadowlessButton(panelX, buttonsY, buttonWidth, 20,
                    Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".add_event"), b -> openAdd()));
            addRenderableWidget(new ShadowlessButton(panelX + buttonWidth + 4, buttonsY, buttonWidth, 20,
                    Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".edit"), b -> openEdit()));
            addRenderableWidget(new ShadowlessButton(panelX + (buttonWidth + 4) * 2, buttonsY, buttonWidth, 20,
                    Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".delete"), b -> deleteSelected()));
        }
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
