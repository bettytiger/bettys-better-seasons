package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.network.SyncCalendarPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Client-side mirror of the server calendar. Holds the last synced snapshot so later phases
 * (HUD, F3 lines, tooltips, calendar screen) can read the date without any server round-trip.
 * Pure data, no rendering — it is safe to touch only from the client.
 */
public final class ClientCalendar {

    private static CalendarDate date;
    private static List<Season> seasons = List.of();
    private static int daysPerSeason = 31;
    private static int ticksPerDay = 24000;
    private static int startYear = 1;
    private static int earlyEnd = 10;
    private static int midEnd = 21;
    private static Set<String> seasonalDimensions = Set.of("minecraft:overworld");
    private static boolean seasonalWeather = true;

    private ClientCalendar() {
    }

    public static void accept(SyncCalendarPacket packet) {
        seasons = List.copyOf(packet.seasons());
        daysPerSeason = packet.daysPerSeason();
        ticksPerDay = packet.ticksPerDay();
        startYear = packet.startYear();
        earlyEnd = packet.earlyEnd();
        midEnd = packet.midEnd();
        seasonalDimensions = Set.copyOf(packet.seasonalDimensions());
        seasonalWeather = packet.seasonalWeather();
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

    public static int getDaysPerSeason() {
        return daysPerSeason;
    }

    public static int getTicksPerDay() {
        return ticksPerDay;
    }

    public static int getStartYear() {
        return startYear;
    }

    public static int getEarlyEnd() {
        return earlyEnd;
    }

    public static int getMidEnd() {
        return midEnd;
    }

    /** Whether the server lists this dimension as following the seasons. */
    /** Whether the server applies each season's weather. */
    public static boolean isSeasonalWeather() {
        return seasonalWeather;
    }

    public static boolean isSeasonalDimension(ResourceLocation dimension) {
        return seasonalDimensions.contains(dimension.toString());
    }
}
