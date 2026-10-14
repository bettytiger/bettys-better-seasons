package net.bettytiger.bbetter_seasons.seasonal;

import java.util.Locale;

/** What happens to a crop when it is out of season. */
public enum OutOfSeasonRule {
    /** Grows normally; the season is tooltip-only. Bonemeal allowed. */
    NONE,
    /** Growth chance multiplied by the configured slow factor. Bonemeal allowed. */
    SLOW,
    /** No growth, but the crop stays alive. Bonemeal blocked. */
    STOP,
    /** The crop withers on its next random tick. Bonemeal blocked. */
    WITHER;

    public static OutOfSeasonRule byName(String name, OutOfSeasonRule fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Whether a crop with this rule may grow at all while out of season. */
    public boolean allowsGrowthOutOfSeason() {
        return this == NONE || this == SLOW;
    }
}
