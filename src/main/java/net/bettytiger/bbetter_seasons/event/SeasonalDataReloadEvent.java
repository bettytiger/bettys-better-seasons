package net.bettytiger.bbetter_seasons.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired after seasonal crop JSON reloads, so consumers can rebuild their own caches. */
public class SeasonalDataReloadEvent extends Event {

    private final MinecraftServer server;

    public SeasonalDataReloadEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
