package net.bettytiger.bbetter_seasons.preset;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.CalendarEventSavedData;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Copies the preset chosen on the Create World screen into the new world's calendar. Runs once, when the
 * integrated server first starts; the choice is dropped if the world turns out not to be brand new.
 */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PresetApplier {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PresetApplier() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        String fileName = PresetSelection.consume();
        if (fileName == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        // A world that has never ticked is one we just created; never touch an existing world.
        if (overworld.getGameTime() != 0L) {
            return;
        }
        CalendarPreset preset = PresetStorage.load(fileName);
        if (preset == null) {
            LOGGER.warn("Calendar preset '{}' was selected but could not be read", fileName);
            return;
        }

        // The singleplayer host owns the events, so they can edit them under OWNER_ONLY permissions.
        GameProfile host = server.getSingleplayerProfile();
        UUID owner = host != null ? host.getId() : null;
        CalendarEventSavedData data = CalendarEventSavedData.get(overworld);
        CalendarManager manager = CalendarManager.get();
        int daysPerSeason = manager != null ? manager.daysPerSeason() : Integer.MAX_VALUE;
        int skipped = 0;
        for (CalendarEvent presetEvent : preset.getEvents()) {
            // A preset made for longer seasons may name days this world doesn't have; such events would never occur.
            if (presetEvent.getDay() < 1 || presetEvent.getDay() > daysPerSeason) {
                skipped++;
                continue;
            }
            data.add(new CalendarEvent(UUID.randomUUID(), presetEvent.getType(), presetEvent.getTitle(),
                    presetEvent.getDescription(), presetEvent.getSeason(), presetEvent.getDay(),
                    presetEvent.getRecurrence(), presetEvent.getYear(), presetEvent.getIcon(), null, owner,
                    presetEvent.getData().copy()));
        }
        LOGGER.info("Applied calendar preset '{}' ({} events) to the new world", preset.getName(), preset.getEvents().size() - skipped);
        if (skipped > 0) {
            LOGGER.warn("Skipped {} event(s) from preset '{}' with a day outside 1-{}", skipped, preset.getName(), daysPerSeason);
        }
    }
}
