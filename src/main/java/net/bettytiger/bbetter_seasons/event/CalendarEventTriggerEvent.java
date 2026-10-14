package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Fired when a calendar event's day begins. Cancel to suppress the consumer's reaction to it. */
@Cancelable
public class CalendarEventTriggerEvent extends Event {

    private final MinecraftServer server;
    private final CalendarEvent calendarEvent;
    private final CalendarDate date;

    public CalendarEventTriggerEvent(MinecraftServer server, CalendarEvent calendarEvent, CalendarDate date) {
        this.server = server;
        this.calendarEvent = calendarEvent;
        this.date = date;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public CalendarEvent getCalendarEvent() {
        return calendarEvent;
    }

    public CalendarDate getDate() {
        return date;
    }
}
