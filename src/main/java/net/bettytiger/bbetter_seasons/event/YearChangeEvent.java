package net.bettytiger.bbetter_seasons.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/** Fired for each new calendar year crossed. */
public class YearChangeEvent extends Event {

    private final MinecraftServer server;
    private final int oldYear;
    private final int newYear;

    public YearChangeEvent(MinecraftServer server, int oldYear, int newYear) {
        this.server = server;
        this.oldYear = oldYear;
        this.newYear = newYear;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public int getOldYear() {
        return oldYear;
    }

    public int getNewYear() {
        return newYear;
    }
}
