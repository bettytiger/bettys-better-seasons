package net.bettytiger.bbetter_seasons.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.Recurrence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named, reusable set of calendar events kept as a JSON file outside any world, so players can build
 * a calendar once and pre-fill new worlds with it. The event format matches {@code event_templates},
 * so a preset file also works as a datapack template.
 */
public final class CalendarPreset {

    private String name;
    /** File name without extension; assigned on first save. */
    @Nullable
    private String fileName;
    private List<CalendarEvent> events;

    public CalendarPreset(String name, @Nullable String fileName, List<CalendarEvent> events) {
        this.name = name;
        this.fileName = fileName;
        this.events = new ArrayList<>(events);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<CalendarEvent> getEvents() {
        return List.copyOf(events);
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = new ArrayList<>(events);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        JsonArray array = new JsonArray();
        for (CalendarEvent event : events) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", event.getType().toString());
            obj.addProperty("title", event.getTitle());
            obj.addProperty("description", event.getDescription());
            obj.addProperty("season", event.getSeason().toString());
            obj.addProperty("day", event.getDay());
            obj.addProperty("recurrence", event.getRecurrence().name());
            obj.addProperty("year", event.getYear());
            array.add(obj);
        }
        json.add("events", array);
        return json;
    }

    public static CalendarPreset fromJson(String fileName, JsonObject json) {
        String name = GsonHelper.getAsString(json, "name", fileName);
        List<CalendarEvent> events = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(json, "events", new JsonArray());
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = GsonHelper.convertToJsonObject(array.get(i), "event");
            events.add(new CalendarEvent(
                    UUID.randomUUID(),
                    new ResourceLocation(GsonHelper.getAsString(obj, "type", "bbetter_seasons:festival")),
                    GsonHelper.getAsString(obj, "title", "Event"),
                    GsonHelper.getAsString(obj, "description", ""),
                    new ResourceLocation(GsonHelper.getAsString(obj, "season")),
                    GsonHelper.getAsInt(obj, "day", 1),
                    Recurrence.valueOf(GsonHelper.getAsString(obj, "recurrence", "REPEAT_YEARLY")),
                    GsonHelper.getAsInt(obj, "year", 1),
                    null, null, null, new CompoundTag()));
        }
        return new CalendarPreset(name, fileName, events);
    }
}
