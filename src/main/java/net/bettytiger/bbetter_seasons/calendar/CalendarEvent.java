package net.bettytiger.bbetter_seasons.calendar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One entry on the calendar. Stored in {@link CalendarEventSavedData} and created either in-game
 * (Phase 5 editor), from datapack templates (Phase 5), or in code via {@code BetterSeasonsAPI.addEvent}.
 */
public class CalendarEvent {

    private final UUID id;
    private final ResourceLocation type;
    private String title;
    private String description;
    private ResourceLocation season;
    private int day;
    private Recurrence recurrence;
    /** Only meaningful for {@link Recurrence#ONE_TIME}. */
    private int year;
    @Nullable
    private ResourceLocation icon;
    @Nullable
    private Integer color;
    /** Owning player, or null for a world event. */
    @Nullable
    private UUID owner;
    /** Free-form data for consumer mods. */
    private CompoundTag data;

    public CalendarEvent(UUID id, ResourceLocation type, String title, String description,
                         ResourceLocation season, int day, Recurrence recurrence, int year,
                         @Nullable ResourceLocation icon, @Nullable Integer color,
                         @Nullable UUID owner, CompoundTag data) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.season = season;
        this.day = day;
        this.recurrence = recurrence;
        this.year = year;
        this.icon = icon;
        this.color = color;
        this.owner = owner;
        this.data = data;
    }

    /** Whether this event occurs on the given date, honoring recurrence. */
    public boolean occursOn(CalendarDate date, ResourceLocation seasonAt) {
        if (day != date.day() || !season.equals(seasonAt)) {
            return false;
        }
        return recurrence == Recurrence.REPEAT_YEARLY || year == date.year();
    }

    public int displayColor() {
        if (color != null) {
            return color;
        }
        return EventTypes.getOrDefault(type).defaultColor();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("type", type.toString());
        tag.putString("title", title);
        tag.putString("description", description);
        tag.putString("season", season.toString());
        tag.putInt("day", day);
        tag.putString("recurrence", recurrence.name());
        tag.putInt("year", year);
        if (icon != null) {
            tag.putString("icon", icon.toString());
        }
        if (color != null) {
            tag.putInt("color", color);
        }
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.put("data", data.copy());
        return tag;
    }

    public static CalendarEvent load(CompoundTag tag) {
        return new CalendarEvent(
                tag.getUUID("id"),
                new ResourceLocation(tag.getString("type")),
                tag.getString("title"),
                tag.getString("description"),
                new ResourceLocation(tag.getString("season")),
                tag.getInt("day"),
                Recurrence.valueOf(tag.getString("recurrence")),
                tag.getInt("year"),
                tag.contains("icon") ? new ResourceLocation(tag.getString("icon")) : null,
                tag.contains("color") ? tag.getInt("color") : null,
                tag.hasUUID("owner") ? tag.getUUID("owner") : null,
                tag.getCompound("data"));
    }

    public UUID getId() {
        return id;
    }

    public ResourceLocation getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ResourceLocation getSeason() {
        return season;
    }

    public int getDay() {
        return day;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public int getYear() {
        return year;
    }

    @Nullable
    public ResourceLocation getIcon() {
        return icon;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public CompoundTag getData() {
        return data;
    }
}
