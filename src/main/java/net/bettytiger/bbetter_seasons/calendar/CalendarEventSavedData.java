package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent list of calendar events, stored on the overworld. */
public class CalendarEventSavedData extends SavedData {

    private static final String DATA_NAME = BettysBetterSeasonsMod.MODID + "_events";

    private final List<CalendarEvent> events = new ArrayList<>();

    public static CalendarEventSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                CalendarEventSavedData::load, CalendarEventSavedData::new, DATA_NAME);
    }

    private static CalendarEventSavedData load(CompoundTag tag) {
        CalendarEventSavedData data = new CalendarEventSavedData();
        ListTag list = tag.getList("events", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.events.add(CalendarEvent.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (CalendarEvent event : events) {
            list.add(event.save());
        }
        tag.put("events", list);
        return tag;
    }

    public List<CalendarEvent> getEvents() {
        return List.copyOf(events);
    }

    public void add(CalendarEvent event) {
        events.add(event);
        setDirty();
    }

    public boolean remove(UUID id) {
        boolean removed = events.removeIf(e -> e.getId().equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
