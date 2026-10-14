package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Client-side black-fade season transition: fade in with "End of &lt;old&gt;", swap to
 * "Beginning of &lt;new&gt;" in the new season's color, then fade back. Driven by real time so it is
 * unaffected by game pausing differences; started by {@code SeasonTransitionPacket}.
 */
public final class SeasonTransition implements IGuiOverlay {

    private static long startMillis;
    private static long durationMillis;
    private static Component oldName = Component.empty();
    private static Component newName = Component.empty();
    private static int newColor = 0xFFFFFF;
    private static boolean active;
    /** Intro mode: no previous season, so show "Year N Season" throughout instead of the End-of/Beginning swap. */
    private static boolean intro;
    private static int introYear;

    private SeasonTransition() {
    }

    public static final SeasonTransition INSTANCE = new SeasonTransition();

    public static void start(Component oldSeasonName, Component newSeasonName, int newSeasonColor) {
        if (!begin()) {
            return;
        }
        intro = false;
        oldName = oldSeasonName;
        newName = newSeasonName;
        newColor = newSeasonColor;
    }

    /** Plays the welcome transition on first entering a world, e.g. "Year 1 Spring". */
    public static void startIntro(Component seasonName, int seasonColor, int year) {
        if (!begin()) {
            return;
        }
        intro = true;
        newName = seasonName;
        newColor = seasonColor;
        introYear = year;
    }

    /** Whether a transition is currently playing (used to hide the chat overlay). */
    public static boolean isActive() {
        return active && (System.currentTimeMillis() - startMillis) < durationMillis;
    }

    private static boolean begin() {
        if (!BetterSeasonsClientConfig.CLIENT.transitionScreen.get()) {
            return false;
        }
        durationMillis = BetterSeasonsClientConfig.CLIENT.transitionDurationTicks.get() * 50L;
        startMillis = System.currentTimeMillis();
        active = true;
        return true;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }
        float t = (System.currentTimeMillis() - startMillis) / (float) durationMillis;
        if (t >= 1.0f) {
            active = false;
            return;
        }

        // Alpha ramps up over the first 15% and down over the last 15%, full black in between.
        float alpha;
        if (t < 0.15f) {
            alpha = t / 0.15f;
        } else if (t > 0.85f) {
            alpha = (1.0f - t) / 0.15f;
        } else {
            alpha = 1.0f;
        }
        int alphaByte = Math.max(0, Math.min(255, (int) (alpha * 255))) << 24;
        graphics.fill(0, 0, screenWidth, screenHeight, alphaByte);

        // Show text only while the screen is dark enough to read it.
        if (t >= 0.12f && t <= 0.88f) {
            Font font = Minecraft.getInstance().font;
            Component line;
            int color;
            if (intro) {
                line = Component.translatable("transition." + BettysBetterSeasonsMod.MODID + ".intro", introYear, newName);
                color = 0xFF000000 | newColor;
            } else {
                boolean firstHalf = t < 0.5f;
                line = firstHalf
                        ? Component.translatable("transition." + BettysBetterSeasonsMod.MODID + ".end", oldName)
                        : Component.translatable("transition." + BettysBetterSeasonsMod.MODID + ".begin", newName);
                color = firstHalf ? 0xFFFFFFFF : (0xFF000000 | newColor);
            }
            graphics.drawCenteredString(font, line, screenWidth / 2, screenHeight / 2 - 4, color);
        }
    }
}
