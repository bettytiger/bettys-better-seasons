package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent calendar state. The only stored value is {@code dayOffset}: the date is otherwise derived
 * from the overworld's day time, so sleeping and {@code /time add} never need special handling and
 * {@code /calendar set} only moves this offset, never the sun.
 */
public class CalendarSavedData extends SavedData {

    private static final String DATA_NAME = BettysBetterSeasonsMod.MODID + "_calendar";

    private long dayOffset = 0L;
    /** Players who have already seen the welcome transition in this world. */
    private final Set<UUID> introShown = new HashSet<>();

    public static CalendarSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                CalendarSavedData::load, CalendarSavedData::new, DATA_NAME);
    }

    private static CalendarSavedData load(CompoundTag tag) {
        CalendarSavedData data = new CalendarSavedData();
        data.dayOffset = tag.getLong("dayOffset");
        ListTag shown = tag.getList("introShown", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < shown.size(); i++) {
            data.introShown.add(net.minecraft.nbt.NbtUtils.loadUUID(shown.get(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("dayOffset", dayOffset);
        ListTag shown = new ListTag();
        for (UUID id : introShown) {
            shown.add(net.minecraft.nbt.NbtUtils.createUUID(id));
        }
        tag.put("introShown", shown);
        return tag;
    }

    public long getDayOffset() {
        return dayOffset;
    }

    public void setDayOffset(long dayOffset) {
        if (this.dayOffset != dayOffset) {
            this.dayOffset = dayOffset;
            setDirty();
        }
    }

    /** Marks the welcome transition as shown for a player; returns true if this was the first time. */
    public boolean markIntroShown(UUID player) {
        if (introShown.add(player)) {
            setDirty();
            return true;
        }
        return false;
    }
}
