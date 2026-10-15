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
@SuppressWarnings("ALL")
public class SeasonManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Code-level fallback used only if no datapack (not even this mod's own) supplies any seasons.
     * Mirrors {@code data/bbetter_seasons/seasons/*.json}.
     */
    public static final List<Season> DEFAULTS = List.of(
            builtIn("spring", 0x7FC241, 0, 0x7CF05A, 0.40f, SeasonWeather.VANILLA),
            builtIn("summer", 0xF2C438, 1, 0x3FA83A, 0.30f, SeasonWeather.VANILLA),
            builtIn("autumn", 0xE07A2B, 2, 0xE05A24, 0.70f, SeasonWeather.VANILLA),
            builtIn("winter", 0x8FC7E8, 3, 0xB4DCF5, 0.85f, SeasonWeather.SNOW)
    );

    private static Season builtIn(String path, int color, int order, int tint, float tintStrength, SeasonWeather weather) {
        return new Season(new ResourceLocation(BettysBetterSeasonsMod.MODID, path),
                "season." + BettysBetterSeasonsMod.MODID + "." + path, color, order, null, tint, tintStrength, weather);
    }

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
        int color = parseColor(json, "color", 0xFFFFFF);
        int order = GsonHelper.getAsInt(json, "order", 0);
        ResourceLocation icon = json.has("icon") ? new ResourceLocation(GsonHelper.getAsString(json, "icon")) : null;
        int tint = parseColor(json, "tint", Season.NO_TINT);
        float tintStrength = Math.max(0.0f, Math.min(1.0f, GsonHelper.getAsFloat(json, "tint_strength", 0.5f)));
        SeasonWeather weather = SeasonWeather.VANILLA;
        if (json.has("weather")) {
            String name = GsonHelper.getAsString(json, "weather");
            weather = SeasonWeather.byName(name);
            if (weather == null) {
                LOGGER.warn("Season '{}' has unknown weather '{}' (expected vanilla, snow, rain or clear); using vanilla",
                        id, name);
                weather = SeasonWeather.VANILLA;
            }
        }
        return new Season(id, translationKey, color, order, icon, tint, tintStrength, weather);
    }

    /** Accepts either a "#RRGGBB" string or a raw integer; returns {@code fallback} when the key is absent. */
    private static int parseColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
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
