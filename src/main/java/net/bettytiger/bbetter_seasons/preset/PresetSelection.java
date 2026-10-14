package net.bettytiger.bbetter_seasons.preset;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The preset chosen on the Create World screen. It is stored by file name and read from disk when the
 * new world first starts, so later edits to the preset are picked up. Each Create World screen starts a
 * fresh session, and the choice is consumed (or dropped) the next time an integrated server starts.
 */
public final class PresetSelection {

    @Nullable
    private static volatile String fileName;
    @Nullable
    private static volatile String displayName;
    private static volatile WeakReference<Object> session = new WeakReference<>(null);

    private PresetSelection() {
    }

    /**
     * Called whenever the Create World screen (re)builds its Game tab. A different screen instance means a
     * new world-creation session, so any earlier choice is cleared; returning from a sub-screen keeps it.
     */
    public static void beginSession(Object createWorldScreen) {
        if (session.get() != createWorldScreen) {
            clear();
            session = new WeakReference<>(createWorldScreen);
        }
    }

    public static void select(CalendarPreset preset) {
        fileName = preset.getFileName();
        displayName = preset.getName();
    }

    public static void clear() {
        fileName = null;
        displayName = null;
    }

    public static boolean isSelected(CalendarPreset preset) {
        return fileName != null && fileName.equals(preset.getFileName());
    }

    @Nullable
    public static String selectedName() {
        return displayName;
    }

    /** Keeps the shown name in sync with the files on disk, and drops a choice whose file was deleted. */
    public static void refresh(List<CalendarPreset> presets) {
        String current = fileName;
        if (current == null) {
            return;
        }
        for (CalendarPreset preset : presets) {
            if (current.equals(preset.getFileName())) {
                displayName = preset.getName();
                return;
            }
        }
        clear();
    }

    /** Returns the chosen preset's file name and clears the choice. */
    @Nullable
    public static String consume() {
        String chosen = fileName;
        clear();
        return chosen;
    }
}
