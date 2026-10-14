package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.calendar.SubSeason;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired when the sub-season changes (Early -> Mid -> Late). */
public class SubSeasonChangeEvent extends Event {

    private final MinecraftServer server;
    private final SubSeason oldSubSeason;
    private final SubSeason newSubSeason;

    public SubSeasonChangeEvent(MinecraftServer server, SubSeason oldSubSeason, SubSeason newSubSeason) {
        this.server = server;
        this.oldSubSeason = oldSubSeason;
        this.newSubSeason = newSubSeason;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public SubSeason getOldSubSeason() {
        return oldSubSeason;
    }

    public SubSeason getNewSubSeason() {
        return newSubSeason;
    }
}
