package net.bettytiger.bbetter_seasons.seasonal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Optional interface for crop blocks that want to define their seasonal behavior in code.
 * Datapack entries in {@code data/<ns>/seasonal/crops/} always take precedence over these defaults,
 * so assignments stay tweakable without recompiling.
 */
public interface ISeasonalCrop {

    /** Default seasons this crop grows in when no datapack entry exists. Empty = every season. */
    default Set<ResourceLocation> getDefaultSeasons() {
        return Set.of();
    }

    /** Default out-of-season rule when no datapack entry exists. */
    default OutOfSeasonRule getDefaultRule() {
        return OutOfSeasonRule.NONE;
    }

    /**
     * Called instead of the default {@code withered_crop} replacement when this crop withers.
     * Used, for example, by two-block crops to remove both halves cleanly.
     */
    default void onOutOfSeason(ServerLevel level, BlockPos pos, BlockState state) {
    }
}
