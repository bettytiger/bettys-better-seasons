package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.bettytiger.bbetter_seasons.network.SyncCalendarPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (absDay != lastAbsDay) {
            lastAbsDay = absDay;
            syncAll();
        }
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
}
