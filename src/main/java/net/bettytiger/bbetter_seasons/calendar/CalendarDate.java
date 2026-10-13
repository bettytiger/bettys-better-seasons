package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * An immutable snapshot of the calendar, derived from the absolute day number. All of the
 * date math lives here so the server and the client compute dates the same way.
 *
 * @param absDay      absolute day number (can be negative); the single value everything is derived from
 * @param day         1-based day within the current season
 * @param seasonIndex index into the ordered season list (not a season id; map it through the season list)
 * @param subSeason   Early / Mid / Late
 * @param weekday     0 = Monday ... 6 = Sunday
 * @param year        calendar year, offset by the configured start year
 */
public record CalendarDate(long absDay, int day, int seasonIndex, SubSeason subSeason, int weekday, int year) {

    /** 0 = Monday, matching GardenTCG's existing MarketCalendar schedule. */
    private static final String[] WEEKDAY_KEYS = {
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    };

    /**
     * Derive a full date from an absolute day number.
     *
     * @param daysPerSeason D, the number of days in each season
     * @param seasonCount   S, the number of seasons
     */
    public static CalendarDate of(long absDay, int daysPerSeason, int seasonCount, int startYear, int earlyEnd, int midEnd) {
        int d = Math.max(1, daysPerSeason);
        int s = Math.max(1, seasonCount);
        int day = (int) Math.floorMod(absDay, (long) d) + 1;
        int seasonIndex = (int) Math.floorMod(Math.floorDiv(absDay, (long) d), (long) s);
        int year = (int) Math.floorDiv(absDay, (long) d * s) + startYear;
        int weekday = (int) Math.floorMod(absDay, 7L);
        SubSeason subSeason = SubSeason.forDay(day, earlyEnd, midEnd);
        return new CalendarDate(absDay, day, seasonIndex, subSeason, weekday, year);
    }

    public MutableComponent weekdayName() {
        return Component.translatable("weekday." + BettysBetterSeasonsMod.MODID + "." + WEEKDAY_KEYS[Math.floorMod(weekday, 7)]);
    }

    /**
     * Human-readable form such as "Monday, Day 1 of Early Spring, Year 1".
     *
     * @param seasons the ordered season list this date's {@link #seasonIndex()} refers to
     */
    public MutableComponent describe(List<Season> seasons) {
        Component seasonName = seasonIndex >= 0 && seasonIndex < seasons.size()
                ? seasons.get(seasonIndex).displayName()
                : Component.literal("?");
        return Component.translatable(
                "chat." + BettysBetterSeasonsMod.MODID + ".date",
                weekdayName(),
                day,
                Component.translatable(subSeason.translationKey()),
                seasonName,
                year
        );
    }
}
