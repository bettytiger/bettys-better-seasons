package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Seasonal grass and leaf colour. Each season's tint peaks in the middle of the season and blends toward the
 * neighbouring seasons on either side, so colours shift a little every day and never jump at a season change.
 * Applied to biome grass and foliage colours (see {@code BiomeColorMixin}) and to birch and spruce leaves.
 */
public final class SeasonalTint {

    /** The colour grass and leaves blend toward, and how strongly (0 = vanilla colours). */
    public record Tint(int color, float strength) {
        public static final Tint NONE = new Tint(0xFFFFFF, 0.0f);
    }

    /** Vanilla plains grass; used to judge whether a change is visible enough to rebuild chunks for. */
    private static final int REFERENCE_GRASS = 0x91BD59;
    private static final int VISIBLE_CHANGE = 3;

    private static volatile Tint current = Tint.NONE;

    private SeasonalTint() {
    }

    /** Blends a vanilla grass or leaf colour toward the current season's tint. Called from colour lookups. */
    public static int apply(int vanillaColor) {
        Tint tint = current;
        return tint.strength() <= 0.0f ? vanillaColor : blend(vanillaColor, tint.color(), tint.strength());
    }

    /**
     * Recomputes the tint from the synced date. Chunk meshes have colours baked in, so when the change is
     * visible the tint cache is cleared and chunks are rebuilt (a brief stutter, at most about once a day).
     */
    public static void refresh() {
        Tint previous = current;
        Tint next = target();
        current = next;
        if (visiblyDifferent(previous, next)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                minecraft.level.clearTintCaches();
                minecraft.levelRenderer.allChanged();
            }
        }
    }

    /** Back to vanilla colours, e.g. when leaving a world. */
    public static void reset() {
        current = Tint.NONE;
    }

    private static Tint target() {
        Minecraft minecraft = Minecraft.getInstance();
        CalendarDate date = ClientCalendar.getDate();
        if (!BetterSeasonsClientConfig.CLIENT.seasonalColors.get() || date == null || minecraft.level == null
                || !ClientCalendar.isSeasonalDimension(minecraft.level.dimension().location())) {
            return Tint.NONE;
        }
        return compute(ClientCalendar.getSeasons(), date.seasonIndex(), date.day(), ClientCalendar.getDaysPerSeason());
    }

    /**
     * The tint on a given day. A season's tint peaks mid-season; between two peaks colour and strength blend
     * linearly, wrapping from the last season back to the first. Seasons without a tint count as strength 0.
     */
    public static Tint compute(List<Season> seasons, int seasonIndex, int day, int daysPerSeason) {
        if (seasons.isEmpty()) {
            return Tint.NONE;
        }
        int days = Math.max(1, daysPerSeason);
        // Position measured in seasons from the first season's mid-point.
        double position = seasonIndex + (day - 0.5) / days - 0.5;
        long fromIndex = (long) Math.floor(position);
        Season from = seasons.get(Math.floorMod(fromIndex, seasons.size()));
        Season to = seasons.get(Math.floorMod(fromIndex + 1, seasons.size()));
        float progress = (float) (position - fromIndex);

        float strength = lerp(strengthOf(from), strengthOf(to), progress);
        if (strength <= 0.0f) {
            return Tint.NONE;
        }
        return new Tint(blend(tintOr(from, to), tintOr(to, from), progress), strength);
    }

    private static float strengthOf(Season season) {
        return season.hasTint() ? season.tintStrength() : 0.0f;
    }

    /** A season's tint, borrowing its neighbour's when it has none (its strength is 0 there anyway). */
    private static int tintOr(Season season, Season neighbour) {
        if (season.hasTint()) {
            return season.tint();
        }
        return neighbour.hasTint() ? neighbour.tint() : 0xFFFFFF;
    }

    private static boolean visiblyDifferent(Tint a, Tint b) {
        int colorA = a.strength() <= 0.0f ? REFERENCE_GRASS : blend(REFERENCE_GRASS, a.color(), a.strength());
        int colorB = b.strength() <= 0.0f ? REFERENCE_GRASS : blend(REFERENCE_GRASS, b.color(), b.strength());
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((colorA >> shift) & 0xFF) - ((colorB >> shift) & 0xFF)) >= VISIBLE_CHANGE) {
                return true;
            }
        }
        return false;
    }

    /** Linear RGB blend from {@code from} toward {@code to}; keeps {@code from}'s alpha byte. */
    static int blend(int from, int to, float amount) {
        int result = from & 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (from >> shift) & 0xFF;
            int b = (to >> shift) & 0xFF;
            result |= (Math.round(a + (b - a) * amount) & 0xFF) << shift;
        }
        return result;
    }

    private static float lerp(float a, float b, float amount) {
        return a + (b - a) * amount;
    }
}
