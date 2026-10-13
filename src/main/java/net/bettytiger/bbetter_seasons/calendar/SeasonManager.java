package net.bettytiger.bbetter_seasons.calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Loads season definitions from {@code data/<ns>/seasons/*.json} on server start and {@code /reload}.
 * The parsed, order-sorted list is held statically for the running server; the client gets its own
 * copy through {@code SyncCalendarPacket} and never reads this listener.
 */
public class SeasonManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    /** Code-level fallback used only if no datapack (not even this mod's own) supplies any seasons. */
    public static final List<Season> DEFAULTS = List.of(
            new Season(new ResourceLocation(BettysBetterSeasonsMod.MODID, "spring"), "season." + BettysBetterSeasonsMod.MODID + ".spring", 0x7FC241, 0, null),
            new Season(new ResourceLocation(BettysBetterSeasonsMod.MODID, "summer"), "season." + BettysBetterSeasonsMod.MODID + ".summer", 0xF2C438, 1, null),
            new Season(new ResourceLocation(BettysBetterSeasonsMod.MODID, "autumn"), "season." + BettysBetterSeasonsMod.MODID + ".autumn", 0xE07A2B, 2, null),
            new Season(new ResourceLocation(BettysBetterSeasonsMod.MODID, "winter"), "season." + BettysBetterSeasonsMod.MODID + ".winter", 0x8FC7E8, 3, null)
    );

    private static volatile List<Season> seasons = DEFAULTS;

    public SeasonManager() {
        super(GSON, "seasons");
    }

    /** The current ordered season list. Never empty. */
    public static List<Season> getSeasons() {
        return seasons;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Season> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                parsed.add(parse(id, GsonHelper.convertToJsonObject(entry.getValue(), "season")));
            } catch (Exception e) {
                LOGGER.error("Skipping invalid season '{}': {}", id, e.getMessage());
            }
        }
        parsed.sort(Comparator.comparingInt(Season::order).thenComparing(s -> s.id().toString()));
        seasons = parsed.isEmpty() ? DEFAULTS : List.copyOf(parsed);
        LOGGER.debug("Loaded {} season(s)", seasons.size());
    }

    private static Season parse(ResourceLocation id, JsonObject json) {
        String translationKey = GsonHelper.getAsString(json, "name", "season." + id.getNamespace() + "." + id.getPath());
        int color = parseColor(json);
        int order = GsonHelper.getAsInt(json, "order", 0);
        ResourceLocation icon = json.has("icon") ? new ResourceLocation(GsonHelper.getAsString(json, "icon")) : null;
        return new Season(id, translationKey, color, order, icon);
    }

    /** Accepts either a "#RRGGBB" string or a raw integer. Defaults to white. */
    private static int parseColor(JsonObject json) {
        if (!json.has("color")) {
            return 0xFFFFFF;
        }
        JsonElement element = json.get("color");
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String s = element.getAsString().trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            return (int) Long.parseLong(s, 16) & 0xFFFFFF;
        }
        return element.getAsInt() & 0xFFFFFF;
    }
}
