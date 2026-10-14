package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Draws the current date in a configurable screen corner and publishes its bounds for other HUDs. */
public class DateHudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        BetterSeasonsClientConfig.Client config = BetterSeasonsClientConfig.CLIENT;

        if (!config.hudEnabled.get() || mc.options.hideGui || mc.options.renderDebug) {
            BetterSeasonsClientAPI.setHudBounds(new BetterSeasonsClientAPI.HudBounds(0, 0, 0, 0));
            return;
        }

        CalendarDate date = ClientCalendar.getDate();
        List<Season> seasons = ClientCalendar.getSeasons();
        if (date == null || seasons.isEmpty()) {
            BetterSeasonsClientAPI.setHudBounds(new BetterSeasonsClientAPI.HudBounds(0, 0, 0, 0));
            return;
        }

        Font font = mc.font;
        Component text = date.describe(seasons);
        double scale = config.hudScale.get();
        int margin = config.hudMargin.get();
        int width = (int) Math.ceil(font.width(text) * scale);
        int height = (int) Math.ceil(font.lineHeight * scale);

        HudCorner corner = config.hudCorner.get();
        int x = corner.isLeft() ? margin : screenWidth - width - margin;
        int y = corner.isTop() ? margin : screenHeight - height - margin;

        int color = seasonColor(date, seasons);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();

        BetterSeasonsClientAPI.setHudBounds(new BetterSeasonsClientAPI.HudBounds(x, y, width, height));
    }

    private static int seasonColor(CalendarDate date, List<Season> seasons) {
        int index = date.seasonIndex();
        if (index >= 0 && index < seasons.size()) {
            return 0xFF000000 | seasons.get(index).color();
        }
        return 0xFFFFFFFF;
    }
}
