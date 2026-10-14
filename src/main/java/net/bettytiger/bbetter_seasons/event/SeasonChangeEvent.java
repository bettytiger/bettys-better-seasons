package net.bettytiger.bbetter_seasons.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired for each season boundary crossed (old and new season ids). */
public class SeasonChangeEvent extends Event {

    private final MinecraftServer server;
    private final ResourceLocation oldSeason;
    private final ResourceLocation newSeason;

    public SeasonChangeEvent(MinecraftServer server, ResourceLocation oldSeason, ResourceLocation newSeason) {
        this.server = server;
        this.oldSeason = oldSeason;
        this.newSeason = newSeason;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ResourceLocation getOldSeason() {
        return oldSeason;
    }

    public ResourceLocation getNewSeason() {
        return newSeason;
    }
}
