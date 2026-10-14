package net.bettytiger.bbetter_seasons.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Reads and writes calendar presets as JSON files in {@code config/bbetter_seasons/presets/}. */
public final class PresetStorage {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String EXTENSION = ".json";

    private PresetStorage() {
    }

    public static Path directory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(BettysBetterSeasonsMod.MODID).resolve("presets");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Could not create calendar preset folder '{}': {}", dir, e.getMessage());
        }
        return dir;
    }

    /** All readable presets, sorted by name. Invalid files are skipped and logged. */
    public static List<CalendarPreset> loadAll() {
        List<CalendarPreset> presets = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory())) {
            files.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .forEach(path -> {
                        CalendarPreset preset = read(path);
                        if (preset != null) {
                            presets.add(preset);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Could not list calendar presets: {}", e.getMessage());
        }
        presets.sort(Comparator.comparing(preset -> preset.getName().toLowerCase(Locale.ROOT)));
        return presets;
    }

    /** Loads a single preset by file name (without extension), or null if it is missing or invalid. */
    @Nullable
    public static CalendarPreset load(String fileName) {
        Path path = directory().resolve(fileName + EXTENSION);
        return Files.isRegularFile(path) ? read(path) : null;
    }

    /** Writes the preset, assigning it a unique file name on first save. Returns false on failure. */
    public static boolean save(CalendarPreset preset) {
        if (preset.getFileName() == null) {
            preset.setFileName(uniqueFileName(preset.getName()));
        }
        Path path = directory().resolve(preset.getFileName() + EXTENSION);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(preset.toJson(), writer);
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not save calendar preset '{}': {}", path, e.getMessage());
            return false;
        }
    }

    public static void delete(CalendarPreset preset) {
        if (preset.getFileName() == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory().resolve(preset.getFileName() + EXTENSION));
        } catch (IOException e) {
            LOGGER.error("Could not delete calendar preset '{}': {}", preset.getFileName(), e.getMessage());
        }
    }

    @Nullable
    private static CalendarPreset read(Path path) {
        String fileName = path.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - EXTENSION.length());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return CalendarPreset.fromJson(stem, GsonHelper.parse(reader));
        } catch (Exception e) {
            LOGGER.error("Skipping invalid calendar preset '{}': {}", fileName, e.getMessage());
            return null;
        }
    }

    private static String uniqueFileName(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "preset";
        }
        Path dir = directory();
        String candidate = base;
        for (int n = 2; Files.exists(dir.resolve(candidate + EXTENSION)); n++) {
            candidate = base + "_" + n;
        }
        return candidate;
    }
}
