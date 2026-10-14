package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.calendar.SeasonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Seasons offered by the event editors. In a world these are the world's synced seasons; on the main
 * menu (editing a preset) no world is loaded, so the built-in seasons are used instead.
 */
public final class SelectableSeasons {

    private SelectableSeasons() {
    }

    /**
     * @param include a season id that must stay selectable (e.g. an existing event's custom season), so
     *                editing an event never silently moves it to another season
     */
    public static List<Season> list(@Nullable ResourceLocation include) {
        boolean inWorld = Minecraft.getInstance().level != null && !ClientCalendar.getSeasons().isEmpty();
        List<Season> seasons = new ArrayList<>(inWorld ? ClientCalendar.getSeasons() : SeasonManager.DEFAULTS);
        if (include != null && seasons.stream().noneMatch(season -> season.id().equals(include))) {
            seasons.add(new Season(include, "season." + include.getNamespace() + "." + include.getPath(),
                    0xFFFFFF, seasons.size(), null));
        }
        return seasons;
    }

    public static Component displayName(ResourceLocation id) {
        for (Season season : list(null)) {
            if (season.id().equals(id)) {
                return season.displayName();
            }
        }
        return Component.literal(id.getPath());
    }
}
