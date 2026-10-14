package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} once per new calendar day. */
public class DayChangeEvent extends Event {

    private final MinecraftServer server;
    private final CalendarDate oldDate;
    private final CalendarDate newDate;
    private final long daysPassed;

    public DayChangeEvent(MinecraftServer server, CalendarDate oldDate, CalendarDate newDate, long daysPassed) {
        this.server = server;
        this.oldDate = oldDate;
        this.newDate = newDate;
        this.daysPassed = daysPassed;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public CalendarDate getOldDate() {
        return oldDate;
    }

    public CalendarDate getNewDate() {
        return newDate;
    }

    /** Days elapsed since the previous update. 1 for a night of sleep, more for time skips. */
    public long getDaysPassed() {
        return daysPassed;
    }
}
