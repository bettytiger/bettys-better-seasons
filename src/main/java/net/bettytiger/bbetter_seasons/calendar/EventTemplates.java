package net.bettytiger.bbetter_seasons.calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.*;

/**
 * Loads event templates from {@code data/<ns>/event_templates/*.json}, each a group of world events to
 * pre-fill a world with. Applied once per template id (tracked in {@link CalendarEventSavedData}), so a
 * player deleting a pre-filled event keeps it gone across loads.
 */
@SuppressWarnings("ALL")
public class EventTemplates extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, List<CalendarEvent>> templates = Map.of();

    public EventTemplates() {
        super(GSON, "event_templates");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, List<CalendarEvent>> parsed = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                parsed.put(entry.getKey(), parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "template")));
            } catch (Exception e) {
                LOGGER.error("Skipping invalid event template '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        templates = Map.copyOf(parsed);
        LOGGER.debug("Loaded {} event template(s)", templates.size());
    }

    private static List<CalendarEvent> parse(ResourceLocation templateId, JsonObject json) {
        List<CalendarEvent> events = new ArrayList<>();
        var array = GsonHelper.getAsJsonArray(json, "events");
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            // Deterministic id so re-applying the same template never duplicates an event.
            UUID id = UUID.nameUUIDFromBytes((templateId + "#" + i).getBytes());
            ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(obj, "type", "bbetter_seasons:festival"));
            String title = GsonHelper.getAsString(obj, "title", "Event");
            String description = GsonHelper.getAsString(obj, "description", "");
            ResourceLocation season = new ResourceLocation(GsonHelper.getAsString(obj, "season"));
            int day = GsonHelper.getAsInt(obj, "day", 1);
            Recurrence recurrence = Recurrence.valueOf(GsonHelper.getAsString(obj, "recurrence", "REPEAT_YEARLY"));
            int year = GsonHelper.getAsInt(obj, "year", 1);
            events.add(new CalendarEvent(id, type, title, description, season, day, recurrence, year, null, null, null, new CompoundTag()));
        }
        return events;
    }

    /** Applies any template not yet applied to this world, adding its events. Idempotent. */
    public static boolean apply(CalendarEventSavedData data) {
        boolean changed = false;
        for (Map.Entry<ResourceLocation, List<CalendarEvent>> entry : templates.entrySet()) {
            String templateId = entry.getKey().toString();
            if (data.hasAppliedTemplate(templateId)) {
                continue;
            }
            for (CalendarEvent event : entry.getValue()) {
                if (data.get(event.getId()) == null) {
                    data.add(event);
                    changed = true;
                }
            }
            data.markTemplateApplied(templateId);
        }
        return changed;
    }
}
