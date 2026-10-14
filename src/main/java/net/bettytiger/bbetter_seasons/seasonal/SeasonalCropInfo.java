package net.bettytiger.bbetter_seasons.seasonal;

import net.bettytiger.bbetter_seasons.calendar.SubSeason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The resolved seasonal behavior for a single crop (block or item), after targets and datapack
 * overrides have been applied. This is the shape both the server gate and the client tooltips use.
 *
 * @param seasons the season ids the crop grows in; empty means every season
 * @param rule    what happens out of season
 * @param growth  optional per-season growth multipliers (1.0 = normal); absent season = 1.0
 * @param peak    optional sub-season bonus, may be null
 */
public record SeasonalCropInfo(Set<ResourceLocation> seasons, OutOfSeasonRule rule,
                               Map<ResourceLocation, Float> growth, @Nullable PeakBonus peak) {

    public boolean growsEverySeason() {
        return seasons.isEmpty();
    }

    public boolean growsIn(ResourceLocation season) {
        return seasons.isEmpty() || seasons.contains(season);
    }

    /** Growth multiplier for the given season and sub-season, combining the per-season value and peak bonus. */
    public float growthMultiplier(ResourceLocation season, SubSeason subSeason) {
        float base = growth.getOrDefault(season, 1.0f);
        if (peak != null && peak.matches(season, subSeason)) {
            base *= peak.growth();
        }
        return base;
    }

    public void writeTo(FriendlyByteBuf buf) {
        buf.writeVarInt(seasons.size());
        for (ResourceLocation season : seasons) {
            buf.writeResourceLocation(season);
        }
        buf.writeEnum(rule);
        buf.writeVarInt(growth.size());
        for (Map.Entry<ResourceLocation, Float> entry : growth.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
        buf.writeBoolean(peak != null);
        if (peak != null) {
            peak.writeTo(buf);
        }
    }

    public static SeasonalCropInfo readFrom(FriendlyByteBuf buf) {
        int seasonCount = buf.readVarInt();
        Set<ResourceLocation> seasons = new HashSet<>(seasonCount);
        for (int i = 0; i < seasonCount; i++) {
            seasons.add(buf.readResourceLocation());
        }
        OutOfSeasonRule rule = buf.readEnum(OutOfSeasonRule.class);
        int growthCount = buf.readVarInt();
        Map<ResourceLocation, Float> growth = new HashMap<>(growthCount);
        for (int i = 0; i < growthCount; i++) {
            growth.put(buf.readResourceLocation(), buf.readFloat());
        }
        PeakBonus peak = buf.readBoolean() ? PeakBonus.readFrom(buf) : null;
        return new SeasonalCropInfo(seasons, rule, growth, peak);
    }
}
