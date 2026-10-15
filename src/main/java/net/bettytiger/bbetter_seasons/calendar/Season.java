package net.bettytiger.bbetter_seasons.calendar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A season definition loaded from {@code data/<ns>/seasons/*.json}. Seasons are referenced everywhere
 * by their {@link #id()}, so renaming or reordering them in data never breaks crop or event references.
 *
 * @param id            the season id (the datapack file's resource location, e.g. {@code bbetter_seasons:spring})
 * @param translationKey client-side display name key, e.g. {@code season.bbetter_seasons.spring}
 * @param color         RGB tint used by the HUD and transition screen (0xRRGGBB)
 * @param order         sort order; lower comes first in the calendar cycle
 * @param icon          optional icon texture, may be null
 * @param tint          grass and leaf colour at the season's peak (0xRRGGBB), or {@link #NO_TINT} to leave
 *                      vanilla colours alone
 * @param tintStrength  how far grass and leaves are blended toward {@code tint}, 0 (vanilla) to 1 (full tint)
 * @param weather       the season's weather rule, e.g. snow all winter
 */
public record Season(ResourceLocation id, String translationKey, int color, int order, @Nullable ResourceLocation icon,
                     int tint, float tintStrength, SeasonWeather weather) {

    public static final int NO_TINT = -1;

    /** A season that leaves grass and leaf colours and the weather unchanged. */
    public Season(ResourceLocation id, String translationKey, int color, int order, @Nullable ResourceLocation icon) {
        this(id, translationKey, color, order, icon, NO_TINT, 0.0f, SeasonWeather.VANILLA);
    }

    public boolean hasTint() {
        return tint != NO_TINT;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey);
    }

    public void writeTo(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeUtf(translationKey);
        buf.writeInt(color);
        buf.writeVarInt(order);
        buf.writeBoolean(icon != null);
        if (icon != null) {
            buf.writeResourceLocation(icon);
        }
        buf.writeInt(tint);
        buf.writeFloat(tintStrength);
        buf.writeEnum(weather);
    }

    public static Season readFrom(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String translationKey = buf.readUtf();
        int color = buf.readInt();
        int order = buf.readVarInt();
        ResourceLocation icon = buf.readBoolean() ? buf.readResourceLocation() : null;
        int tint = buf.readInt();
        float tintStrength = buf.readFloat();
        SeasonWeather weather = buf.readEnum(SeasonWeather.class);
        return new Season(id, translationKey, color, order, icon, tint, tintStrength, weather);
    }
}
