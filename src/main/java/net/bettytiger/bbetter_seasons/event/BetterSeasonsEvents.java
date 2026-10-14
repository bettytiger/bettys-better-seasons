package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.calendar.SeasonManager;
import net.bettytiger.bbetter_seasons.command.BetterSeasonsCommands;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-bus wiring for the calendar: lifecycle, tick, datapack reload/sync, and commands. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BetterSeasonsEvents {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CalendarManager.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CalendarManager.onServerStopped();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        CalendarManager manager = CalendarManager.get();
        if (manager != null) {
            manager.tick();
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SeasonManager());
        event.addListener(new SeasonalCropRegistry());
    }

    /** Fires on player login (per-player) and after {@code /reload} (null player = everyone). */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        CalendarManager manager = CalendarManager.get();
        if (manager == null) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            manager.syncTo(player);
            manager.syncSeasonalTo(player);
            manager.syncEventsTo(player);
        } else {
            // /reload: seasonal JSON was re-read, so let consumers rebuild caches, then re-sync everyone.
            MinecraftForge.EVENT_BUS.post(new SeasonalDataReloadEvent(manager.overworld().getServer()));
            manager.syncAll();
            manager.syncSeasonalAll();
            manager.syncEventsAll();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterSeasonsCommands.register(event.getDispatcher());
    }
}
