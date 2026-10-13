package net.bettytiger.bbetter_seasons.calendar;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent calendar state. The only stored value is {@code dayOffset}: the date is otherwise derived
 * from the overworld's day time, so sleeping and {@code /time add} never need special handling and
 * {@code /calendar set} only moves this offset, never the sun.
 */
public class CalendarSavedData extends SavedData {

    private static final String DATA_NAME = BettysBetterSeasonsMod.MODID + "_calendar";

    private long dayOffset = 0L;

    public static CalendarSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                CalendarSavedData::load, CalendarSavedData::new, DATA_NAME);
    }

    private static CalendarSavedData load(CompoundTag tag) {
        CalendarSavedData data = new CalendarSavedData();
        data.dayOffset = tag.getLong("dayOffset");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("dayOffset", dayOffset);
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
}
