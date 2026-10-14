package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.bettytiger.bbetter_seasons.event.CalendarEventTriggerEvent;
import net.bettytiger.bbetter_seasons.event.DayChangeEvent;
import net.bettytiger.bbetter_seasons.event.SeasonChangeEvent;
import net.bettytiger.bbetter_seasons.event.SubSeasonChangeEvent;
import net.bettytiger.bbetter_seasons.event.YearChangeEvent;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.bettytiger.bbetter_seasons.network.SyncCalendarPacket;
import net.bettytiger.bbetter_seasons.network.SyncSeasonalPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-side single source of truth for the calendar. One instance exists per running server,
 * created on server start and discarded on stop. The date is computed from the overworld day time
 * plus the stored offset; nothing is counted tick by tick.
 */
public class CalendarManager {

    @Nullable
    private static CalendarManager instance;

    private final MinecraftServer server;
    /** Last absolute day seen by {@link #tick()}; used to detect day crossings. */
    private long lastAbsDay = Long.MIN_VALUE;

    private CalendarManager(MinecraftServer server) {
        this.server = server;
    }

    public static void onServerStarting(MinecraftServer server) {
        instance = new CalendarManager(server);
    }

    public static void onServerStopped() {
        instance = null;
    }

    @Nullable
    public static CalendarManager get() {
        return instance;
    }

    // --- Config-backed rules ---

    public int daysPerSeason() {
        return BetterSeasonsConfig.SERVER.daysPerSeason.get();
    }

    public int ticksPerDay() {
        return BetterSeasonsConfig.SERVER.ticksPerDay.get();
    }

    public int startYear() {
        return BetterSeasonsConfig.SERVER.startYear.get();
    }

    public int earlyEnd() {
        return BetterSeasonsConfig.SERVER.subSeasonEarlyEnd.get();
    }

    public int midEnd() {
        return BetterSeasonsConfig.SERVER.subSeasonMidEnd.get();
    }

    public List<Season> seasons() {
        return SeasonManager.getSeasons();
    }

    // --- State ---

    public ServerLevel overworld() {
        return server.overworld();
    }

    public CalendarSavedData savedData() {
        return CalendarSavedData.get(overworld());
    }

    /** The portion of the absolute day that comes from world time, before the stored offset. */
    private long worldBaseDay() {
        return Math.floorDiv(overworld().getDayTime(), ticksPerDay());
    }

    public long currentAbsDay() {
        return savedData().getDayOffset() + worldBaseDay();
    }

    public CalendarDate currentDate() {
        return CalendarDate.of(currentAbsDay(), daysPerSeason(), seasons().size(), startYear(), earlyEnd(), midEnd());
    }

    // --- Tick / crossing detection ---

    /**
     * Called each server tick. When the absolute day changes, re-syncs clients. Phase 2 will fire the
     * Forge day/season/year events here, once per crossing, with the number of days passed.
     */
    public void tick() {
        long absDay = currentAbsDay();
        if (absDay == lastAbsDay) {
            return;
        }
        long previous = lastAbsDay;
        lastAbsDay = absDay;
        syncAll();
        if (previous != Long.MIN_VALUE) {
            fireChangeEvents(previous, absDay);
        }
    }

    /** Caps how many boundary events a single huge time skip may fire, to avoid pathological loops. */
    private static final long MAX_BOUNDARY_EVENTS = 256;

    private void fireChangeEvents(long old, long now) {
        int d = daysPerSeason();
        int s = seasons().size();
        CalendarDate oldDate = dateOf(old);
        CalendarDate newDate = dateOf(now);

        MinecraftForge.EVENT_BUS.post(new DayChangeEvent(server, oldDate, newDate, now - old));

        // Only enumerate forward-moving boundaries; backward jumps (admin edits) just re-sync.
        if (now <= old) {
            return;
        }

        if (oldDate.subSeason() != newDate.subSeason()) {
            MinecraftForge.EVENT_BUS.post(new SubSeasonChangeEvent(server, oldDate.subSeason(), newDate.subSeason()));
        }

        long oldSeasonAbs = Math.floorDiv(old, (long) d);
        long seasonCrossings = Math.floorDiv(now, (long) d) - oldSeasonAbs;
        for (long k = 1; k <= Math.min(seasonCrossings, MAX_BOUNDARY_EVENTS); k++) {
            long boundaryAbs = (oldSeasonAbs + k) * d;
            MinecraftForge.EVENT_BUS.post(new SeasonChangeEvent(server, seasonIdAt(boundaryAbs - 1), seasonIdAt(boundaryAbs)));
        }

        long cycle = (long) d * s;
        long oldYearAbs = Math.floorDiv(old, cycle);
        long yearCrossings = Math.floorDiv(now, cycle) - oldYearAbs;
        for (long k = 1; k <= Math.min(yearCrossings, MAX_BOUNDARY_EVENTS); k++) {
            int newYear = (int) (oldYearAbs + k) + startYear();
            MinecraftForge.EVENT_BUS.post(new YearChangeEvent(server, newYear - 1, newYear));
        }

        triggerCalendarEvents(newDate);
    }

    private void triggerCalendarEvents(CalendarDate date) {
        for (CalendarEvent event : BetterSeasonsAPI.getEventsOn(overworld(), date)) {
            MinecraftForge.EVENT_BUS.post(new CalendarEventTriggerEvent(server, event, date));
        }
    }

    private CalendarDate dateOf(long absDay) {
        return CalendarDate.of(absDay, daysPerSeason(), seasons().size(), startYear(), earlyEnd(), midEnd());
    }

    @Nullable
    private ResourceLocation seasonIdAt(long absDay) {
        int index = dateOf(absDay).seasonIndex();
        List<Season> list = seasons();
        return index >= 0 && index < list.size() ? list.get(index).id() : null;
    }

    // --- Commands ---

    /** Converts a (year, season index, day) triple into an absolute day number. */
    public long toAbsDay(int year, int seasonIndex, int day) {
        int d = daysPerSeason();
        int s = seasons().size();
        return (long) (year - startYear()) * d * s + (long) seasonIndex * d + (day - 1L);
    }

    /** Moves the calendar to the given absolute day by adjusting the stored offset only. */
    public void setAbsDay(long targetAbsDay) {
        savedData().setDayOffset(targetAbsDay - worldBaseDay());
        lastAbsDay = targetAbsDay;
        syncAll();
    }

    // --- Sync ---

    public SyncCalendarPacket buildPacket() {
        return new SyncCalendarPacket(currentAbsDay(), daysPerSeason(), startYear(), earlyEnd(), midEnd(), seasons());
    }

    public void syncAll() {
        BetterSeasonsNetwork.sendToAll(buildPacket());
    }

    public void syncTo(ServerPlayer player) {
        BetterSeasonsNetwork.sendTo(player, buildPacket());
    }

    public SyncSeasonalPacket buildSeasonalPacket() {
        return new SyncSeasonalPacket(BetterSeasonsAPI.buildSeasonalSyncData());
    }

    public void syncSeasonalAll() {
        BetterSeasonsNetwork.sendToAll(buildSeasonalPacket());
    }

    public void syncSeasonalTo(ServerPlayer player) {
        BetterSeasonsNetwork.sendTo(player, buildSeasonalPacket());
    }
}
