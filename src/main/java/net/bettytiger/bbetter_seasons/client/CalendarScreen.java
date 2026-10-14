package net.bettytiger.bbetter_seasons.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Full-screen host for the {@link CalendarPanel}, opened by the handheld item, wall block, or API. */
public class CalendarScreen extends Screen {

    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 190;

    public CalendarScreen() {
        super(Component.translatable("screen.bbetter_seasons.calendar"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(this.width - 40, MAX_WIDTH);
        int panelHeight = Math.min(this.height - 40, MAX_HEIGHT);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        addRenderableWidget(new CalendarPanel(panelX, panelY, panelWidth, panelHeight));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
