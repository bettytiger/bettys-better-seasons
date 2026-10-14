package net.bettytiger.bbetter_seasons.client;

/**
 * Client-only public API. Phase 3 exposes the date HUD's bounds so other mods' HUDs (e.g. a money
 * overlay) can stack around it. Phase 4 will add {@code openCalendar()} and {@code createCalendarPanel()}.
 */
public final class BetterSeasonsClientAPI {

    /** Screen-pixel rectangle the date HUD last drew into. Zero-size when the HUD is hidden. */
    public record HudBounds(int x, int y, int width, int height) {
        public boolean isEmpty() {
            return width <= 0 || height <= 0;
        }
    }

    private static volatile HudBounds hudBounds = new HudBounds(0, 0, 0, 0);

    private BetterSeasonsClientAPI() {
    }

    public static HudBounds getHudBounds() {
        return hudBounds;
    }

    static void setHudBounds(HudBounds bounds) {
        hudBounds = bounds;
    }

    /** Opens the full calendar screen. */
    public static void openCalendar() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CalendarScreen());
    }

    /** The month grid as a widget other screens can embed (e.g. a consumer's computer app). */
    public static CalendarPanel createCalendarPanel(int x, int y, int width, int height) {
        return new CalendarPanel(x, y, width, height);
    }
}

