package net.bettytiger.bbetter_seasons.seasonal;

import net.bettytiger.bbetter_seasons.calendar.SubSeason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * An optional growth bonus during one sub-season of one season, e.g. Mid Winter grows faster.
 *
 * @param season    the season this bonus applies in
 * @param subSeason the sub-season within it
 * @param growth    multiplier applied on top of the normal growth rate (should be &gt; 1 to speed up)
 */
public record PeakBonus(ResourceLocation season, SubSeason subSeason, float growth) {

    public boolean matches(ResourceLocation currentSeason, SubSeason currentSubSeason) {
        return season.equals(currentSeason) && subSeason == currentSubSeason;
    }

    public void writeTo(FriendlyByteBuf buf) {
        buf.writeResourceLocation(season);
        buf.writeEnum(subSeason);
        buf.writeFloat(growth);
    }

    public static PeakBonus readFrom(FriendlyByteBuf buf) {
        return new PeakBonus(buf.readResourceLocation(), buf.readEnum(SubSeason.class), buf.readFloat());
    }
}
