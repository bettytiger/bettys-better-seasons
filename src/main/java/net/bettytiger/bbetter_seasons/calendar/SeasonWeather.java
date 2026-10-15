package net.bettytiger.bbetter_seasons.calendar;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** A season's weather rule, set with {@code "weather"} in the season's datapack JSON. */
public enum SeasonWeather {
    /** Vanilla's weather cycle. */
    VANILLA,
    /** Precipitates the whole season, and rain falls as snow (biomes that never rain, like deserts, stay dry). */
    SNOW,
    /** Rains the whole season; cold biomes still snow as usual. */
    RAIN,
    /** Clear skies the whole season. */
    CLEAR;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a JSON value such as {@code "snow"}; null when unknown. */
    @Nullable
    public static SeasonWeather byName(String name) {
        for (SeasonWeather weather : values()) {
            if (weather.serializedName().equals(name.trim().toLowerCase(Locale.ROOT))) {
                return weather;
            }
        }
        return null;
    }

    /** Whether this rule keeps it precipitating all season. */
    public boolean forcesPrecipitation() {
        return this == SNOW || this == RAIN;
    }
}
