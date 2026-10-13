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
 */
public record Season(ResourceLocation id, String translationKey, int color, int order, @Nullable ResourceLocation icon) {

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
    }

    public static Season readFrom(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String translationKey = buf.readUtf();
        int color = buf.readInt();
        int order = buf.readVarInt();
        ResourceLocation icon = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new Season(id, translationKey, color, order, icon);
    }
}
