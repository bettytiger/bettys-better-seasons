package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.network.SyncCalendarPacket;

import java.util.List;

/**
 * Client-side mirror of the server calendar. Holds the last synced snapshot so later phases
 * (HUD, F3 lines, tooltips, calendar screen) can read the date without any server round-trip.
 * Pure data, no rendering — it is safe to touch only from the client.
 */
public final class ClientCalendar {

    private static CalendarDate date;
    private static List<Season> seasons = List.of();

    private ClientCalendar() {
    }

    public static void accept(SyncCalendarPacket packet) {
        seasons = List.copyOf(packet.seasons());
        date = CalendarDate.of(
                packet.absDay(),
                packet.daysPerSeason(),
                seasons.size(),
                packet.startYear(),
                packet.earlyEnd(),
                packet.midEnd());
    }

    /** The current date, or null before the first sync. */
    public static CalendarDate getDate() {
        return date;
    }

    public static List<Season> getSeasons() {
        return seasons;
    }
}
