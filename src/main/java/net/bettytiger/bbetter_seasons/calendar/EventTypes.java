package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime registry of {@link EventType}s. Built-in types are registered on load; consumer mods add
 * their own by listening to {@code RegisterEventTypesEvent}.
 */
@SuppressWarnings("ALL")
public final class EventTypes {

    private static final Map<ResourceLocation, EventType> TYPES = new LinkedHashMap<>();

    public static final EventType BIRTHDAY = register(new EventType(id("birthday"), 0xF06EC2, null));
    public static final EventType FESTIVAL = register(new EventType(id("festival"), 0xF2C438, null));
    public static final EventType HOLIDAY = register(new EventType(id("holiday"), 0x5BC85B, null));
    public static final EventType ANNIVERSARY = register(new EventType(id("anniversary"), 0x7FA9FF, null));
    public static final EventType GENERAL = register(new EventType(id("general"), 0x7FA9FF, null));
    public static final EventType SOCIAL_GATHERING = register(new EventType(id("social_gathering"), 0x7FA9FF, null));
    public static final EventType FORMAL = register(new EventType(id("formal"), 0x7FA9FF, null));
    public static final EventType PARTY = register(new EventType(id("party"), 0x7FA9FF, null));

    private EventTypes() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BettysBetterSeasonsMod.MODID, path);
    }

    public static EventType register(EventType type) {
        TYPES.put(type.id(), type);
        return type;
    }

    @Nullable
    public static EventType get(ResourceLocation id) {
        return TYPES.get(id);
    }

    /** The type for an id, falling back to {@link #FESTIVAL} if unknown so events always render. */
    public static EventType getOrDefault(ResourceLocation id) {
        return TYPES.getOrDefault(id, FESTIVAL);
    }

    public static Collection<EventType> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
