package net.bettytiger.bbetter_seasons.calendar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of calendar event (birthday, festival, ...). Keyed by {@link ResourceLocation} in a registry
 * rather than a Java enum, so other mods can add their own types at runtime and still use it like an enum.
 *
 * @param id          the type id, e.g. {@code bbetter_seasons:birthday}
 * @param defaultColor fallback display color (0xRRGGBB) for events of this type
 * @param defaultIcon  optional fallback icon
 */
public record EventType(ResourceLocation id, int defaultColor, @Nullable ResourceLocation defaultIcon) {

    public String translationKey() {
        return "event_type." + id.getNamespace() + "." + id.getPath();
    }
}
