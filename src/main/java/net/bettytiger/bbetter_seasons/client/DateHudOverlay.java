package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Draws the current date in a configurable screen corner and publishes its bounds for other HUDs. */
public class DateHudOverlay implements IGuiOverlay {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(BettysBetterSeasonsMod.MODID, "textures/gui/date_hud.png");
    private static final int BG_TEX_WIDTH = 256;
    private static final int BG_TEX_HEIGHT = 64;
    private static final int PADDING = 4;

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
        int index = date.seasonIndex();
        Component seasonName = index >= 0 && index < seasons.size()
                ? seasons.get(index).displayName()
                : Component.literal("?");
        // Line 1: "Monday, 1 Year 1"   Line 2: "Early Spring Season"
        Component line1 = Component.translatable("hud." + BettysBetterSeasonsMod.MODID + ".line1",
                date.weekdayName(), date.day(), date.year());
        Component line2 = Component.translatable("hud." + BettysBetterSeasonsMod.MODID + ".line2",
                Component.translatable(date.subSeason().translationKey()), seasonName);

        double scale = config.hudScale.get();
        int margin = config.hudMargin.get();
        int lineHeight = font.lineHeight;
        // Background box wraps the two text lines with padding.
        int boxW = Math.max(font.width(line1), font.width(line2)) + PADDING * 2;
        int boxH = lineHeight * 2 + PADDING * 2;
        int width = (int) Math.ceil(boxW * scale);
        int height = (int) Math.ceil(boxH * scale);

        HudCorner corner = config.hudCorner.get();
        int x = corner.isLeft() ? margin : screenWidth - width - margin;
        int y = corner.isTop() ? margin : screenHeight - height - margin;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0f);
        graphics.blit(BACKGROUND, 0, 0, boxW, boxH, 0.0f, 0.0f, BG_TEX_WIDTH, BG_TEX_HEIGHT, BG_TEX_WIDTH, BG_TEX_HEIGHT);
        graphics.drawString(font, line1, PADDING, PADDING, 0xFF000000, false);
        graphics.drawString(font, line2, PADDING, PADDING + lineHeight, 0xFF000000, false);
        graphics.pose().popPose();

        BetterSeasonsClientAPI.setHudBounds(new BetterSeasonsClientAPI.HudBounds(x, y, width, height));
    }
}
