package net.bettytiger.bbetter_seasons.weather;

import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.calendar.SeasonWeather;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seasonal weather. Each tick the server keeps seasonal dimensions raining (or clear) as the current season's
 * {@link SeasonWeather} asks, and during a {@link SeasonWeather#SNOW snow} season rain falls as snow: the mixins
 * turn {@code RAIN} into {@code SNOW} for rendering, rain sounds, {@link Level#isRainingAt} and cauldron filling.
 * Snow does not pile up on the ground; this only changes what falls.
 */
public final class SeasonalWeather {

    /** Rain time is topped back up to this when it runs low, so the weather ends shortly after the season does. */
    private static final int PRECIPITATION_TICKS = 2400;
    private static final int TOP_UP_BELOW = 600;

    /** Server dimensions where rain currently falls as snow. */
    private static volatile Set<ResourceKey<Level>> snowingDimensions = Set.of();
    /** Whether rain falls as snow in the client's current level (set from the synced calendar). */
    private static volatile boolean clientSnow;
    /** Set around vanilla code that asks a biome for its precipitation, so that one call site can be converted. */
    private static final ThreadLocal<Boolean> CONVERTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SeasonalWeather() {
    }

    /** Whether rain falls as snow in this level right now. */
    public static boolean isSnowing(Level level) {
        return level.isClientSide ? clientSnow : snowingDimensions.contains(level.dimension());
    }

    /** Whether rain falls as snow in the client's current level. */
    public static boolean isClientSnowing() {
        return clientSnow;
    }

    public static void setClientSnow(boolean snow) {
        clientSnow = snow;
    }

    /** Starts converting precipitation on this thread; pair with {@link #endConversion()}. */
    public static void beginConversion(boolean snow) {
        CONVERTING.set(snow);
    }

    public static void endConversion() {
        CONVERTING.set(Boolean.FALSE);
    }

    /** Rain becomes snow inside a conversion; everything else is unchanged. Called from {@code Biome}. */
    public static Biome.Precipitation convert(Biome.Precipitation precipitation) {
        return precipitation == Biome.Precipitation.RAIN && CONVERTING.get() ? Biome.Precipitation.SNOW : precipitation;
    }

    /** The current season's weather rule, or vanilla when seasonal weather is off. */
    public static SeasonWeather ruleFor(List<Season> seasons, int seasonIndex, boolean enabled) {
        if (!enabled || seasonIndex < 0 || seasonIndex >= seasons.size()) {
            return SeasonWeather.VANILLA;
        }
        return seasons.get(seasonIndex).weather();
    }

    /** Called every server tick (after the calendar ticks). */
    public static void tickServer(CalendarManager manager) {
        SeasonWeather rule = ruleFor(manager.seasons(), manager.currentDate().seasonIndex(),
                BetterSeasonsConfig.SERVER.seasonalWeather.get());
        List<? extends String> dimensions = BetterSeasonsConfig.SERVER.seasonalDimensions.get();
        Set<ResourceKey<Level>> snowing = rule == SeasonWeather.SNOW ? new HashSet<>() : Set.of();
        for (ServerLevel level : manager.overworld().getServer().getAllLevels()) {
            if (!dimensions.contains(level.dimension().location().toString())) {
                continue;
            }
            if (rule == SeasonWeather.SNOW) {
                snowing.add(level.dimension());
            }
            enforce(level, rule);
        }
        if (!snowing.equals(snowingDimensions)) {
            snowingDimensions = Set.copyOf(snowing);
        }
    }

    private static void enforce(ServerLevel level, SeasonWeather rule) {
        // doWeatherCycle false means someone locked the weather; leave it alone.
        if (rule == SeasonWeather.VANILLA || !level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)
                || !(level.getLevelData() instanceof ServerLevelData data)) {
            return;
        }
        // Dimensions sharing the overworld's weather (the Nether, the End) ignore these setters.
        if (rule.forcesPrecipitation()) {
            if (!data.isRaining() || data.getRainTime() < TOP_UP_BELOW || data.getClearWeatherTime() > 0) {
                data.setClearWeatherTime(0);
                data.setRaining(true);
                data.setRainTime(PRECIPITATION_TICKS);
            }
        } else if (data.isRaining() || data.isThundering() || data.getClearWeatherTime() < TOP_UP_BELOW) {
            data.setRaining(false);
            data.setThundering(false);
            data.setClearWeatherTime(PRECIPITATION_TICKS);
        }
    }

    /** Clears server state when the server stops (singleplayer worlds share the JVM with the client). */
    public static void resetServer() {
        snowingDimensions = Set.of();
    }
}
