package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.SeasonWeather;
import net.bettytiger.bbetter_seasons.weather.SeasonalWeather;
import net.minecraft.client.Minecraft;

/** Client side of seasonal weather: decides whether rain falls as snow in the level the player is in. */
public final class ClientWeather {

    private ClientWeather() {
    }

    /** Recomputes from the synced calendar; called on each sync and on respawn or dimension change. */
    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        CalendarDate date = ClientCalendar.getDate();
        boolean snow = date != null && minecraft.level != null
                && ClientCalendar.isSeasonalDimension(minecraft.level.dimension().location())
                && SeasonalWeather.ruleFor(ClientCalendar.getSeasons(), date.seasonIndex(),
                        ClientCalendar.isSeasonalWeather()) == SeasonWeather.SNOW;
        SeasonalWeather.setClientSnow(snow);
    }

    public static void reset() {
        SeasonalWeather.setClientSnow(false);
    }
}
