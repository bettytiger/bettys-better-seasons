package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Client-side copy of the calendar event list, for the calendar screen. */
public final class ClientEvents {

    private static List<CalendarEvent> events = List.of();

    private ClientEvents() {
    }

    public static void set(List<CalendarEvent> list) {
        events = List.copyOf(list);
    }

    public static List<CalendarEvent> all() {
        return events;
    }

    /** Events that occur on the given season/day, honoring recurrence against the current date. */
    public static List<CalendarEvent> on(CalendarDate date, ResourceLocation season, int day) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (event.getDay() == day && event.getSeason().equals(season)
                    && (event.getRecurrence() == net.bettytiger.bbetter_seasons.calendar.Recurrence.REPEAT_YEARLY
                    || event.getYear() == date.year())) {
                result.add(event);
            }
        }
        return result;
    }
}
