package net.bettytiger.bbetter_seasons.seasonal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.calendar.SeasonManager;
import net.bettytiger.bbetter_seasons.calendar.SubSeason;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads seasonal crop entries from {@code data/<ns>/seasonal/crops/*.json} and resolves their targets
 * (blocks, block tags, items) into fast lookup maps. Seeds and {@link BlockItem}s inherit their block's
 * entry automatically. Target resolution is done lazily after a reload, when block tags are bound.
 */
@SuppressWarnings("ALL")
public class SeasonalCropRegistry extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile List<RawEntry> rawEntries = List.of();
    @Nullable
    private static volatile Map<Block, SeasonalCropInfo> blockIndex;
    @Nullable
    private static volatile Map<Item, SeasonalCropInfo> itemIndex;
    /** The season list the indices were validated against; a different list means seasons reloaded. */
    @Nullable
    private static volatile List<Season> indexedSeasons;

    public SeasonalCropRegistry() {
        super(GSON, "seasonal/crops");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<RawEntry> parsed = new ArrayList<>();
        entries.entrySet().stream()
                // Stable order so "replace" and last-wins semantics are deterministic.
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    try {
                        parsed.add(parse(e.getKey(), GsonHelper.convertToJsonObject(e.getValue(), "crop entry")));
                    } catch (Exception ex) {
                        LOGGER.error("Skipping invalid seasonal crop entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                });
        rawEntries = List.copyOf(parsed);
        // Drop cached indices; rebuilt lazily on next lookup, by which point tags are bound.
        blockIndex = null;
        itemIndex = null;
        LOGGER.debug("Loaded {} seasonal crop entrie(s)", rawEntries.size());
    }

    // --- Lookup ---

    @Nullable
    public static SeasonalCropInfo getForBlock(Block block) {
        return blockIndex().get(block);
    }

    @Nullable
    public static SeasonalCropInfo getForItem(Item item) {
        return itemIndex().get(item);
    }

    /** All resolved block entries (immutable). Used to build the client sync payload. */
    public static Map<Block, SeasonalCropInfo> blockEntries() {
        return blockIndex();
    }

    /** All resolved item entries (immutable), including explicit item targets. */
    public static Map<Item, SeasonalCropInfo> itemEntries() {
        return itemIndex();
    }

    private static Map<Block, SeasonalCropInfo> blockIndex() {
        if (isStale()) {
            build();
        }
        return blockIndex;
    }

    private static Map<Item, SeasonalCropInfo> itemIndex() {
        if (isStale()) {
            build();
        }
        return itemIndex;
    }

    /** Stale after a crop reload (indices cleared) or a season reload (season list replaced). */
    private static boolean isStale() {
        return blockIndex == null || itemIndex == null || indexedSeasons != SeasonManager.getSeasons();
    }

    private static synchronized void build() {
        if (!isStale()) {
            return;
        }
        List<Season> currentSeasons = SeasonManager.getSeasons();
        Set<ResourceLocation> knownSeasons = new HashSet<>();
        for (Season season : currentSeasons) {
            knownSeasons.add(season.id());
        }

        Map<Block, SeasonalCropInfo> blocks = new HashMap<>();
        Map<Item, SeasonalCropInfo> items = new HashMap<>();
        for (RawEntry raw : rawEntries) {
            Set<ResourceLocation> seasons = withoutUnknownSeasons(raw.source, raw.info.seasons(), knownSeasons);
            SeasonalCropInfo info = seasons == raw.info.seasons()
                    ? raw.info
                    : new SeasonalCropInfo(seasons, raw.info.rule(), raw.info.growth(), raw.info.peak());
            for (Target target : raw.targets) {
                target.resolveBlocks().forEach(block -> {
                    blocks.put(block, info);
                    // Seeds / BlockItems inherit their block's entry (asItem() is AIR when the block has none).
                    Item blockItem = block.asItem();
                    if (blockItem != Items.AIR) {
                        items.putIfAbsent(blockItem, info);
                    }
                });
                target.resolveItems().forEach(item -> items.put(item, info));
            }
        }
        blockIndex = Map.copyOf(blocks);
        itemIndex = Map.copyOf(items);
        indexedSeasons = currentSeasons;
    }

    /**
     * Drops season ids that don't exist (typos, or seasons a pack removed). If every listed season is unknown
     * the entry falls back to every season, so a mistake can't leave a crop permanently out of season.
     * Returns {@code listed} itself when nothing needed dropping.
     */
    static Set<ResourceLocation> withoutUnknownSeasons(ResourceLocation source, Set<ResourceLocation> listed,
                                                       Set<ResourceLocation> knownSeasons) {
        if (listed.isEmpty()) {
            return listed;
        }
        Set<ResourceLocation> valid = new HashSet<>();
        List<ResourceLocation> unknown = new ArrayList<>();
        for (ResourceLocation season : listed) {
            if (knownSeasons.contains(season)) {
                valid.add(season);
            } else {
                unknown.add(season);
            }
        }
        if (unknown.isEmpty()) {
            return listed;
        }
        if (valid.isEmpty()) {
            LOGGER.warn("Seasonal crop entry '{}' only names unknown season(s) {}; treating it as growing in every season",
                    source, unknown);
            return Set.of();
        }
        LOGGER.warn("Seasonal crop entry '{}' names unknown season(s) {}; ignoring them", source, unknown);
        return Set.copyOf(valid);
    }

    // --- Parsing ---

    private static RawEntry parse(ResourceLocation source, JsonObject json) {
        List<Target> targets = new ArrayList<>();
        JsonArray targetArray = GsonHelper.getAsJsonArray(json, "targets");
        for (JsonElement element : targetArray) {
            targets.add(Target.parse(element.getAsString()));
        }

        Set<ResourceLocation> seasons = parseSeasons(json.get("seasons"));

        OutOfSeasonRule rule = OutOfSeasonRule.byName(GsonHelper.getAsString(json, "out_of_season", "none"), OutOfSeasonRule.NONE);

        Map<ResourceLocation, Float> growth = new HashMap<>();
        if (json.has("growth")) {
            JsonObject growthObject = GsonHelper.getAsJsonObject(json, "growth");
            for (Map.Entry<String, JsonElement> e : growthObject.entrySet()) {
                growth.put(new ResourceLocation(e.getKey()), e.getValue().getAsFloat());
            }
        }

        PeakBonus peak = null;
        if (json.has("peak")) {
            JsonObject peakObject = GsonHelper.getAsJsonObject(json, "peak");
            ResourceLocation peakSeason = new ResourceLocation(GsonHelper.getAsString(peakObject, "season"));
            SubSeason sub = SubSeason.valueOf(GsonHelper.getAsString(peakObject, "sub_season", "mid").toUpperCase(java.util.Locale.ROOT));
            float peakGrowth = GsonHelper.getAsFloat(peakObject, "growth", 1.0f);
            peak = new PeakBonus(peakSeason, sub, peakGrowth);
        }

        boolean replace = GsonHelper.getAsBoolean(json, "replace", false);
        return new RawEntry(source, targets, new SeasonalCropInfo(seasons, rule, Map.copyOf(growth), peak), replace);
    }

    /**
     * Reads {@code "seasons"}. Every season: omitted, {@code []}, {@code "all"}, or an array containing
     * {@code "all"}. Otherwise one season id as a string, or an array of ids.
     */
    static Set<ResourceLocation> parseSeasons(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Set.of();
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString().trim();
            return "all".equalsIgnoreCase(value) ? Set.of() : Set.of(new ResourceLocation(value));
        }
        Set<ResourceLocation> seasons = new HashSet<>();
        for (JsonElement entry : GsonHelper.convertToJsonArray(element, "seasons")) {
            String value = entry.getAsString().trim();
            if ("all".equalsIgnoreCase(value)) {
                return Set.of();
            }
            seasons.add(new ResourceLocation(value));
        }
        return Set.copyOf(seasons);
    }

    private record RawEntry(ResourceLocation source, List<Target> targets, SeasonalCropInfo info, boolean replace) {
    }

    /** A single target spec: a block id, a {@code #block tag}, or an item id (prefixed {@code item:}). */
    private record Target(ResourceLocation id, Kind kind) {
        enum Kind {BLOCK, BLOCK_TAG, ITEM}

        static Target parse(String spec) {
            if (spec.startsWith("#")) {
                return new Target(new ResourceLocation(spec.substring(1)), Kind.BLOCK_TAG);
            }
            if (spec.startsWith("item:")) {
                return new Target(new ResourceLocation(spec.substring("item:".length())), Kind.ITEM);
            }
            return new Target(new ResourceLocation(spec), Kind.BLOCK);
        }

        List<Block> resolveBlocks() {
            return switch (kind) {
                case BLOCK -> BuiltInRegistries.BLOCK.containsKey(id)
                        ? List.of(BuiltInRegistries.BLOCK.get(id))
                        : List.of();
                case BLOCK_TAG -> {
                    TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
                    List<Block> result = new ArrayList<>();
                    BuiltInRegistries.BLOCK.getTag(tag).ifPresent(set -> set.forEach(holder -> result.add(holder.value())));
                    yield result;
                }
                case ITEM -> List.of();
            };
        }

        List<Item> resolveItems() {
            if (kind == Kind.ITEM && BuiltInRegistries.ITEM.containsKey(id)) {
                return List.of(BuiltInRegistries.ITEM.get(id));
            }
            return List.of();
        }
    }
}
