package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;

/**
 * A sub-season is derived from the day-of-season and the configured split points.
 * Default splits: Early = days 1-10, Mid = 11-21, Late = 22-31.
 */
public enum SubSeason {
    EARLY,
    MID,
    LATE;

    /** @param day 1-based day of the season. */
    public static SubSeason forDay(int day, int earlyEnd, int midEnd) {
        if (day <= earlyEnd) {
            return EARLY;
        }
        if (day <= midEnd) {
            return MID;
        }
        return LATE;
    }

    /** Translation key for display, e.g. {@code subseason.bbetter_seasons.early}. */
    public String translationKey() {
        return "subseason." + BettysBetterSeasonsMod.MODID + "." + name().toLowerCase();
    }
}
