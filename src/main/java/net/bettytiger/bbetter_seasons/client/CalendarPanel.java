package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.EventTypes;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The season month grid as a reusable widget. Added to {@link CalendarScreen} and embeddable in any
 * other screen (a consumer mod's computer app, for example) via {@code BetterSeasonsClientAPI.createCalendarPanel}.
 * Read-only in Phase 4; the add/edit/delete controls arrive with the Phase 5 editor.
 */
@SuppressWarnings("ALL")
public class CalendarPanel extends AbstractWidget {

    private static final int COLS = 7;
    private static final int PADDING = 6;
    private static final int HEADER_H = 14;
    private static final int WEEKDAY_H = 10;
    private static final int SIDEBAR_W = 108;
    private static final int ICON_SIZE = 8;

    /** The background is authored at this size; it is stretched to the panel. */
    private static final int BG_WIDTH = 320;
    private static final int BG_HEIGHT = 190;
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(BettysBetterSeasonsMod.MODID, "textures/gui/calendar_background.png");
    private static final ResourceLocation GENERAL_ICON =
            new ResourceLocation(BettysBetterSeasonsMod.MODID, "textures/gui/general_icon.png");
    /** Resolved event-type icon per type id (falls back to general_icon when none exists). */
    private static final Map<ResourceLocation, ResourceLocation> ICON_CACHE = new HashMap<>();

    private int viewedSeason = -1;
    private int selectedDay = -1;
    private boolean showCropList = true;

    public CalendarPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    private record Layout(int gridX, int gridY, int cellW, int cellH, int rows,
                          int[] leftArrow, int[] rightArrow, int sidebarX, int sidebarW) {
    }

    private Layout layout(int daysPerSeason) {
        boolean sidebar = getWidth() >= 230;
        int sidebarW = sidebar ? SIDEBAR_W : 0;
        int gridAreaW = getWidth() - PADDING * 2 - sidebarW;
        int gridAreaH = getHeight() - PADDING * 2 - HEADER_H - WEEKDAY_H;
        int rows = Math.max(1, (daysPerSeason + COLS - 1) / COLS);
        int cellW = Math.max(8, gridAreaW / COLS);
        int cellH = Math.max(8, gridAreaH / rows);
        int gridX = getX() + PADDING;
        int gridY = getY() + PADDING + HEADER_H + WEEKDAY_H;
        int[] left = {getX() + PADDING, getY() + PADDING, 10, HEADER_H - 2};
        int[] right = {getX() + PADDING + gridAreaW - 10, getY() + PADDING, 10, HEADER_H - 2};
        int sidebarX = getX() + PADDING + gridAreaW + PADDING;
        return new Layout(gridX, gridY, cellW, cellH, rows, left, right, sidebarX, sidebarW);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        graphics.blit(BACKGROUND, getX(), getY(), getWidth(), getHeight(), 0.0f, 0.0f, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        CalendarDate today = ClientCalendar.getDate();
        List<Season> seasons = ClientCalendar.getSeasons();
        if (today == null || seasons.isEmpty()) {
            centered(graphics, font, Component.literal("Calendar unavailable"),
                    getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, 0xFFAAAAAA);
            return;
        }

        if (viewedSeason < 0 || viewedSeason >= seasons.size()) {
            viewedSeason = Math.floorMod(today.seasonIndex(), seasons.size());
        }
        Season season = seasons.get(viewedSeason);
        int daysPerSeason = Math.max(1, ClientCalendar.getDaysPerSeason());
        Layout l = layout(daysPerSeason);

        // Header: arrows + season name + year.
        text(graphics, font, Component.literal("◀"), l.leftArrow()[0], l.leftArrow()[1] + 2, 0xFF000000);
        text(graphics, font, Component.literal("▶"), l.rightArrow()[0] + 2, l.rightArrow()[1] + 2, 0xFF000000);
        Component heading = Component.empty().append(season.displayName())
                .append(Component.literal(", Year " + today.year()));
        centered(graphics, font, heading, l.gridX() + (l.cellW() * COLS) / 2, getY() + PADDING + 2,
                0xFF000000 | season.color());

        // Weekday labels.
        String[] weekdays = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
        int weekdayY = getY() + PADDING + HEADER_H;
        for (int c = 0; c < COLS; c++) {
            int hx = l.gridX() + c * l.cellW();
            graphics.renderOutline(hx, weekdayY, l.cellW(), WEEKDAY_H, 0xFF000000);
            centered(graphics, font, weekdays[c], hx + l.cellW() / 2, weekdayY + (WEEKDAY_H - font.lineHeight) / 2, 0xFF000000);
        }

        // Day grid.
        boolean viewingCurrentSeason = viewedSeason == Math.floorMod(today.seasonIndex(), seasons.size());
        List<Component> hoverTooltip = null;
        for (int day = 1; day <= daysPerSeason; day++) {
            int col = (day - 1) % COLS;
            int row = (day - 1) / COLS;
            int cx = l.gridX() + col * l.cellW();
            int cy = l.gridY() + row * l.cellH();
            boolean hovered = mouseX >= cx && mouseX < cx + l.cellW() && mouseY >= cy && mouseY < cy + l.cellH();

            boolean isToday = viewingCurrentSeason && day == today.day();
            // Highlight today / hover only; let the background texture show through other cells.
            if (isToday) {
                graphics.fill(cx + 1, cy + 1, cx + l.cellW() - 1, cy + l.cellH() - 1, 0x60FFE08A);
            } else if (hovered) {
                graphics.fill(cx + 1, cy + 1, cx + l.cellW() - 1, cy + l.cellH() - 1, 0x40FFFFFF);
            }
            // 1px border per cell (no top edge) for visibility; today's border pulses purple.
            borderNoTop(graphics, cx, cy, l.cellW(), l.cellH(), isToday ? pulsingPurple() : 0xFF000000);
            if (day == selectedDay) {
                graphics.renderOutline(cx + 1, cy + 1, l.cellW() - 2, l.cellH() - 2, 0xFF6FD3FF);
            }
            text(graphics, font, Integer.toString(day), cx + 2, cy + 2, 0xFF000000);

            List<CalendarEvent> dayEvents = ClientEvents.on(today, season.id(), day);
            if (!dayEvents.isEmpty()) {
                ResourceLocation icon = iconFor(dayEvents.get(0).getType());
                int iconX = cx + (l.cellW() - ICON_SIZE) / 2;
                int iconY = cy + (l.cellH() - ICON_SIZE) / 2;
                graphics.blit(icon, iconX, iconY, 0.0f, 0.0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                if (hovered) {
                    hoverTooltip = eventTooltip(dayEvents, season);
                }
            }
        }

        if (l.sidebarW() > 0) {
            renderSidebar(graphics, font, l, today, seasons, mouseX, mouseY);
        }

        if (hoverTooltip != null) {
            graphics.renderComponentTooltip(font, hoverTooltip, mouseX, mouseY);
        }
    }

    private void renderSidebar(GuiGraphics graphics, Font font, Layout l, CalendarDate today, List<Season> seasons, int mouseX, int mouseY) {
        int x = l.sidebarX();
        int y = getY() + PADDING;
        int lineH = font.lineHeight + 2;
        int groupGap = 8;
        int black = 0xFF000000;
        Level level = Minecraft.getInstance().level;

        // Next season countdown.
        int nextIndex = (today.seasonIndex() + 1) % seasons.size();
        Season nextSeason = seasons.get(nextIndex);
        int until = level != null ? BetterSeasonsAPI.daysUntil(level, nextSeason.id()) : -1;
        if (until >= 0) {
            text(graphics, font, Component.literal(until + " days until"), x, y, black);
            y += lineH;
            text(graphics, font, nextSeason.displayName(), x, y, 0xFF000000 | nextSeason.color());
            y += lineH + groupGap;
        }

        // Next upcoming event.
        CalendarEvent next = nextUpcomingEvent(today, seasons);
        text(graphics, font, Component.literal("Next event:"), x, y, black);
        y += lineH;
        text(graphics, font, next != null ? Component.literal(next.getTitle()) : Component.literal("—"), x, y, black);
        y += lineH + groupGap;

        // In-season crop list (header toggles the list).
        Component cropHeader = Component.literal((showCropList ? "▼ " : "▶ ") + "In season now");
        text(graphics, font, cropHeader, x, y, black);
        y += lineH + 2;
        if (showCropList) {
            int shown = 0;
            for (Item item : inSeasonItems(level, seasons.get(Math.floorMod(today.seasonIndex(), seasons.size())).id())) {
                if (shown >= 6) {
                    text(graphics, font, Component.literal("…"), x + 4, y, black);
                    break;
                }
                text(graphics, font, item.getDescription(), x + 4, y, black);
                y += lineH;
                shown++;
            }
            if (shown == 0) {
                text(graphics, font, Component.literal("—"), x + 4, y, black);
            }
        }
    }

    private List<Item> inSeasonItems(Level level, ResourceLocation currentSeason) {
        List<Item> result = new ArrayList<>();
        if (level == null) {
            return result;
        }
        for (Item item : ForgeRegistries.ITEMS) {
            SeasonalCropInfo info = ClientSeasonal.getForItem(item);
            if (info != null && !info.seasons().isEmpty() && info.seasons().contains(currentSeason)) {
                result.add(item);
            }
        }
        return result;
    }

    private CalendarEvent nextUpcomingEvent(CalendarDate today, List<Season> seasons) {
        int daysPerSeason = Math.max(1, ClientCalendar.getDaysPerSeason());
        int cycle = daysPerSeason * seasons.size();
        CalendarEvent best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (CalendarEvent event : ClientEvents.all()) {
            int seasonIndex = seasonIndexOf(seasons, event.getSeason());
            if (seasonIndex < 0) {
                continue;
            }
            int eventPos = seasonIndex * daysPerSeason + (event.getDay() - 1);
            int todayPos = today.seasonIndex() * daysPerSeason + (today.day() - 1);
            int distance = Math.floorMod(eventPos - todayPos, cycle);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = event;
            }
        }
        return best;
    }

    private static int seasonIndexOf(List<Season> seasons, ResourceLocation id) {
        for (int i = 0; i < seasons.size(); i++) {
            if (seasons.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private List<Component> eventTooltip(List<CalendarEvent> events, Season season) {
        List<Component> tooltip = new ArrayList<>();
        for (CalendarEvent event : events) {
            tooltip.add(Component.literal(event.getTitle()).withStyle(s -> s.withColor(event.displayColor())));
            tooltip.add(Component.translatable(EventTypes.getOrDefault(event.getType()).translationKey())
                    .withStyle(s -> s.withColor(0x8890A0)));
            if (!event.getDescription().isBlank()) {
                tooltip.add(Component.literal(event.getDescription()).withStyle(s -> s.withColor(0xBBBBBB)));
            }
        }
        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        List<Season> seasons = ClientCalendar.getSeasons();
        if (seasons.isEmpty()) {
            return false;
        }
        Layout l = layout(Math.max(1, ClientCalendar.getDaysPerSeason()));
        if (inBox(mouseX, mouseY, l.leftArrow())) {
            viewedSeason = Math.floorMod(viewedSeason - 1, seasons.size());
            return true;
        }
        if (inBox(mouseX, mouseY, l.rightArrow())) {
            viewedSeason = Math.floorMod(viewedSeason + 1, seasons.size());
            return true;
        }
        // Toggle the crop list by clicking anywhere in the sidebar column.
        if (l.sidebarW() > 0 && mouseX >= l.sidebarX() && mouseX < getX() + getWidth()) {
            showCropList = !showCropList;
            return true;
        }
        // Select a day cell.
        int daysPerSeason = Math.max(1, ClientCalendar.getDaysPerSeason());
        for (int day = 1; day <= daysPerSeason; day++) {
            int col = (day - 1) % COLS;
            int row = (day - 1) / COLS;
            int cx = l.gridX() + col * l.cellW();
            int cy = l.gridY() + row * l.cellH();
            if (mouseX >= cx && mouseX < cx + l.cellW() && mouseY >= cy && mouseY < cy + l.cellH()) {
                selectedDay = day;
                return true;
            }
        }
        return false;
    }

    public net.minecraft.resources.ResourceLocation getViewedSeasonId() {
        List<Season> seasons = ClientCalendar.getSeasons();
        if (seasons.isEmpty()) {
            return null;
        }
        int index = viewedSeason >= 0 && viewedSeason < seasons.size() ? viewedSeason : 0;
        return seasons.get(index).id();
    }

    public int getSelectedDay() {
        return selectedDay;
    }

    public CalendarEvent getSelectedEvent() {
        CalendarDate today = ClientCalendar.getDate();
        ResourceLocation seasonId = getViewedSeasonId();
        if (today == null || seasonId == null || selectedDay < 1) {
            return null;
        }
        List<CalendarEvent> events = ClientEvents.on(today, seasonId, selectedDay);
        return events.isEmpty() ? null : events.get(0);
    }

    private static boolean inBox(double mx, double my, int[] box) {
        return mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3];
    }

    /** Draws a 1px cell border without the top edge (left, right, bottom only). */
    private static void borderNoTop(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        g.fill(x, y, x + 1, y + h, color);           // left
        g.fill(x + w - 1, y, x + w, y + h, color);   // right
    }

    /** A purple border color whose alpha pulses over time, for the current day. */
    private static int pulsingPurple() {
        float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 250.0));
        int alpha = 0x50 + (int) (pulse * 0xAF);
        return (alpha << 24) | 0xA020F0;
    }

    // Text helpers that draw without a drop shadow (over the custom background).
    private static void text(GuiGraphics g, Font f, Component c, int x, int y, int color) {
        g.drawString(f, c, x, y, color, false);
    }

    private static void text(GuiGraphics g, Font f, String s, int x, int y, int color) {
        g.drawString(f, s, x, y, color, false);
    }

    private static void centered(GuiGraphics g, Font f, Component c, int centerX, int y, int color) {
        g.drawString(f, c, centerX - f.width(c) / 2, y, color, false);
    }

    private static void centered(GuiGraphics g, Font f, String s, int centerX, int y, int color) {
        g.drawString(f, s, centerX - f.width(s) / 2, y, color, false);
    }

    /** Icon for an event type: {@code textures/gui/<path>_icon.png}, or {@code general_icon} if absent. */
    private static ResourceLocation iconFor(ResourceLocation eventType) {
        return ICON_CACHE.computeIfAbsent(eventType, type -> {
            ResourceLocation candidate = new ResourceLocation(type.getNamespace(),
                    "textures/gui/" + type.getPath() + "_icon.png");
            return Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent()
                    ? candidate : GENERAL_ICON;
        });
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal("Calendar"));
    }
}
