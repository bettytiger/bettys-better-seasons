package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Announces the day's (and upcoming) calendar events in chat when the day changes. Server-side. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatNotifications {

    @SubscribeEvent
    public static void onDayChange(DayChangeEvent event) {
        if (!BetterSeasonsConfig.SERVER.chatNotifications.get()) {
            return;
        }
        CalendarManager manager = CalendarManager.get();
        if (manager == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        CalendarDate today = event.getNewDate();
        int leadDays = BetterSeasonsConfig.SERVER.notificationLeadDays.get();

        for (int offset = 0; offset <= leadDays; offset++) {
            CalendarDate date = manager.dateOf(today.absDay() + offset);
            for (CalendarEvent calendarEvent : BetterSeasonsAPI.getEventsOn(manager.overworld(), date)) {
                Component line = offset == 0
                        ? Component.translatable("chat." + BettysBetterSeasonsMod.MODID + ".event_today", calendarEvent.getTitle())
                        : Component.translatable("chat." + BettysBetterSeasonsMod.MODID + ".event_in_days", calendarEvent.getTitle(), offset);
                server.getPlayerList().broadcastSystemMessage(line, false);
            }
        }
    }
}
