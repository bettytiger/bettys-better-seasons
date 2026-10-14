package net.bettytiger.bbetter_seasons.api;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.CalendarEventSavedData;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.calendar.SeasonManager;
import net.bettytiger.bbetter_seasons.client.ClientCalendar;
import net.bettytiger.bbetter_seasons.client.ClientSeasonal;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.bettytiger.bbetter_seasons.event.CropSeasonCheckEvent;
import net.bettytiger.bbetter_seasons.registry.ModRegistry;
import net.bettytiger.bbetter_seasons.seasonal.ISeasonalCrop;
import net.bettytiger.bbetter_seasons.seasonal.SeasonExemption;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropInfo;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public, side-safe entry point for consumer mods. Server calls read the authoritative
 * {@link CalendarManager} and {@link SeasonalCropRegistry}; client calls read the synced
 * {@link ClientCalendar} / {@link ClientSeasonal} copies.
 */
public final class BetterSeasonsAPI {

    private static final List<SeasonExemption> EXEMPTIONS = new CopyOnWriteArrayList<>();
    private static final float CLIENT_SLOW_FALLBACK = 0.25f;

    private BetterSeasonsAPI() {
    }

    // --- Dates & events ---

    /** The current date, or null if the calendar isn't ready (no server, or client not yet synced). */
    @Nullable
    public static CalendarDate getDate(Level level) {
        if (level.isClientSide) {
            return ClientCalendar.getDate();
        }
        CalendarManager manager = CalendarManager.get();
        return manager != null ? manager.currentDate() : null;
    }

    public static List<Season> getSeasons(Level level) {
        return level.isClientSide ? ClientCalendar.getSeasons() : SeasonManager.getSeasons();
    }

    /** The id of the season on the given date, or null if unavailable. */
    @Nullable
    public static ResourceLocation getCurrentSeasonId(Level level) {
        CalendarDate date = getDate(level);
        List<Season> seasons = getSeasons(level);
        if (date == null || date.seasonIndex() < 0 || date.seasonIndex() >= seasons.size()) {
            return null;
        }
        return seasons.get(date.seasonIndex()).id();
    }

    /** Events occurring on the given date. Server-side only; returns empty on the client. */
    public static List<CalendarEvent> getEventsOn(Level level, CalendarDate date) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        List<Season> seasons = getSeasons(level);
        if (date.seasonIndex() < 0 || date.seasonIndex() >= seasons.size()) {
            return List.of();
        }
        ResourceLocation seasonAt = seasons.get(date.seasonIndex()).id();
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : CalendarEventSavedData.get(serverLevel.getServer().overworld()).getEvents()) {
            if (event.occursOn(date, seasonAt)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Reconstructs the calendar date for an arbitrary absolute day number, using the current calendar
     * rules. Handy for turning a stored {@code absDay} (e.g. a shipping-log entry) back into a date.
     * Returns null if the calendar isn't ready.
     */
    @Nullable
    public static CalendarDate dateFor(Level level, long absDay) {
        if (level.isClientSide) {
            List<Season> seasons = ClientCalendar.getSeasons();
            if (seasons.isEmpty()) {
                return null;
            }
            return CalendarDate.of(absDay, ClientCalendar.getDaysPerSeason(), seasons.size(),
                    ClientCalendar.getStartYear(), ClientCalendar.getEarlyEnd(), ClientCalendar.getMidEnd());
        }
        CalendarManager manager = CalendarManager.get();
        return manager != null ? manager.dateOf(absDay) : null;
    }

    /** Compact "Spring 9, Year 1" label for a date. */
    public static net.minecraft.network.chat.Component formatShort(Level level, CalendarDate date) {
        return date.shortLabel(getSeasons(level));
    }

    /** Compact "Spring 9, Year 1" label for a stored absolute day, or null if the calendar isn't ready. */
    @Nullable
    public static net.minecraft.network.chat.Component formatShort(Level level, long absDay) {
        CalendarDate date = dateFor(level, absDay);
        return date != null ? date.shortLabel(getSeasons(level)) : null;
    }

    /** Days until day 1 of the given season (0 if today is day 1 of it). -1 if unavailable. */
    public static int daysUntil(Level level, ResourceLocation season) {
        List<Season> seasons = getSeasons(level);
        CalendarDate date = getDate(level);
        if (date == null || seasons.isEmpty()) {
            return -1;
        }
        int target = -1;
        for (int i = 0; i < seasons.size(); i++) {
            if (seasons.get(i).id().equals(season)) {
                target = i;
                break;
            }
        }
        if (target < 0) {
            return -1;
        }
        int d = daysPerSeason(level);
        int cycle = d * seasons.size();
        int current = date.seasonIndex() * d + (date.day() - 1);
        int targetStart = target * d;
        return Math.floorMod(targetStart - current, cycle);
    }

    /** Adds an event to the world's calendar and syncs the change to all players. */
    public static void addEvent(ServerLevel level, CalendarEvent event) {
        CalendarEventSavedData.get(level.getServer().overworld()).add(event);
        syncEvents();
    }

    /** Removes an event and, if one was removed, syncs the change to all players. */
    public static boolean removeEvent(ServerLevel level, UUID id) {
        boolean removed = CalendarEventSavedData.get(level.getServer().overworld()).remove(id);
        if (removed) {
            syncEvents();
        }
        return removed;
    }

    private static void syncEvents() {
        CalendarManager manager = CalendarManager.get();
        if (manager != null) {
            manager.syncEventsAll();
        }
    }

    // --- Seasonal crops ---

    /** Season ids a block grows in. Empty means every season or not seasonal. */
    public static Set<ResourceLocation> getSeasonsFor(Level level, Block block) {
        SeasonalCropInfo info = infoForBlock(level, block);
        return info != null ? info.seasons() : Set.of();
    }

    /** Season ids an item's crop grows in. Empty means every season or not seasonal. */
    public static Set<ResourceLocation> getSeasonsFor(Level level, Item item) {
        SeasonalCropInfo info = infoForItem(level, item);
        return info != null ? info.seasons() : Set.of();
    }

    /** Whether the crop at this position is currently in season, honoring exemptions and dimension rules. */
    public static boolean isInSeason(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide && !BetterSeasonsConfig.SERVER.seasonalCropsEnabled.get()) {
            return true;
        }
        SeasonalCropInfo info = infoForBlock(level, state.getBlock());
        if (info == null) {
            return true;
        }
        if (!isSeasonalDimension(level)) {
            return true;
        }
        if (level.getBlockState(pos.below()).is(ModRegistry.IGNORES_SEASONS)) {
            return true;
        }
        for (SeasonExemption exemption : EXEMPTIONS) {
            if (exemption.isExempt(level, pos, state)) {
                return true;
            }
        }
        ResourceLocation current = getCurrentSeasonId(level);
        if (current == null) {
            return true;
        }
        if (!level.isClientSide) {
            CropSeasonCheckEvent event = new CropSeasonCheckEvent(level, pos, state, current);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.getResult() == Event.Result.ALLOW) {
                return true;
            }
            if (event.getResult() == Event.Result.DENY) {
                return false;
            }
        }
        return info.growsIn(current);
    }

    /** Position-independent in-season check, for shops and tooltips. */
    public static boolean isInSeason(Level level, Item item) {
        SeasonalCropInfo info = infoForItem(level, item);
        if (info == null) {
            return true;
        }
        ResourceLocation current = getCurrentSeasonId(level);
        return current == null || info.growsIn(current);
    }

    /** False for {@code stop}/{@code wither} crops out of season; true otherwise. */
    public static boolean canGrow(Level level, BlockPos pos, BlockState state) {
        SeasonalCropInfo info = infoForBlock(level, state.getBlock());
        if (info == null) {
            return true;
        }
        if (isInSeason(level, pos, state)) {
            return true;
        }
        return info.rule().allowsGrowthOutOfSeason();
    }

    /**
     * The growth-chance multiplier to apply at this position: the per-season/peak value in season, or
     * the out-of-season consequence ({@code none}=1, {@code slow}=slowFactor, {@code stop}/{@code wither}=0).
     */
    public static float getGrowthModifier(Level level, BlockPos pos, BlockState state) {
        SeasonalCropInfo info = infoForBlock(level, state.getBlock());
        if (info == null) {
            return 1.0f;
        }
        ResourceLocation current = getCurrentSeasonId(level);
        CalendarDate date = getDate(level);
        if (current == null || date == null) {
            return 1.0f;
        }
        if (isInSeason(level, pos, state)) {
            return info.growthMultiplier(current, date.subSeason());
        }
        return switch (info.rule()) {
            case NONE -> 1.0f;
            case SLOW -> slowFactor(level);
            case STOP, WITHER -> 0.0f;
        };
    }

    public static void registerExemption(SeasonExemption exemption) {
        EXEMPTIONS.add(exemption);
    }

    // --- Server helpers (used internally for syncing) ---

    /** Builds the block/item -> info map sent to clients, including {@link ISeasonalCrop} defaults. */
    public static Map<ResourceLocation, SeasonalCropInfo> buildSeasonalSyncData() {
        Map<ResourceLocation, SeasonalCropInfo> out = new HashMap<>();
        SeasonalCropRegistry.blockEntries().forEach((block, info) -> {
            out.put(ForgeRegistries.BLOCKS.getKey(block), info);
            if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                out.putIfAbsent(ForgeRegistries.ITEMS.getKey(block.asItem()), info);
            }
        });
        SeasonalCropRegistry.itemEntries().forEach((item, info) -> out.put(ForgeRegistries.ITEMS.getKey(item), info));
        // Blocks that define their seasons in code but have no datapack entry.
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof ISeasonalCrop && SeasonalCropRegistry.getForBlock(block) == null) {
                SeasonalCropInfo info = codeDefaults((ISeasonalCrop) block);
                ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                out.put(key, info);
                if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                    out.putIfAbsent(ForgeRegistries.ITEMS.getKey(block.asItem()), info);
                }
            }
        }
        return out;
    }

    // --- Internals ---

    @Nullable
    private static SeasonalCropInfo infoForBlock(Level level, Block block) {
        if (level.isClientSide) {
            return ClientSeasonal.getForBlock(block);
        }
        SeasonalCropInfo info = SeasonalCropRegistry.getForBlock(block);
        if (info == null && block instanceof ISeasonalCrop crop) {
            return codeDefaults(crop);
        }
        return info;
    }

    @Nullable
    private static SeasonalCropInfo infoForItem(Level level, Item item) {
        if (level.isClientSide) {
            return ClientSeasonal.getForItem(item);
        }
        return SeasonalCropRegistry.getForItem(item);
    }

    private static SeasonalCropInfo codeDefaults(ISeasonalCrop crop) {
        return new SeasonalCropInfo(Set.copyOf(crop.getDefaultSeasons()), crop.getDefaultRule(), Map.of(), null);
    }

    private static boolean isSeasonalDimension(Level level) {
        // The dimension list is server config; on the client, don't gate tooltips by dimension.
        if (level.isClientSide) {
            return true;
        }
        String dim = level.dimension().location().toString();
        return BetterSeasonsConfig.SERVER.seasonalDimensions.get().contains(dim);
    }

    private static int daysPerSeason(Level level) {
        if (level.isClientSide) {
            return ClientCalendar.getDaysPerSeason();
        }
        CalendarManager manager = CalendarManager.get();
        return manager != null ? manager.daysPerSeason() : 31;
    }

    private static float slowFactor(Level level) {
        return level.isClientSide ? CLIENT_SLOW_FALLBACK : BetterSeasonsConfig.SERVER.slowFactor.get().floatValue();
    }
}
