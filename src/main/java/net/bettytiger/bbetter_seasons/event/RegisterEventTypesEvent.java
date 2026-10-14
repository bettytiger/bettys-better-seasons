package net.bettytiger.bbetter_seasons.event;

import net.bettytiger.bbetter_seasons.calendar.EventType;
import net.bettytiger.bbetter_seasons.calendar.EventTypes;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} during common setup so consumer
 * mods can register their own {@link EventType}s. Built-in types are already registered before this fires.
 */
public class RegisterEventTypesEvent extends Event {

    public EventType register(EventType type) {
        return EventTypes.register(type);
    }
}
