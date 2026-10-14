package net.bettytiger.bbetter_seasons.seasonal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rule that can force a crop to be treated as always in season, used for greenhouses and similar.
 * Registered through {@code BetterSeasonsAPI.registerExemption}. Example: "has a glass roof within 5 blocks".
 */
@FunctionalInterface
public interface SeasonExemption {
    boolean isExempt(Level level, BlockPos pos, BlockState state);
}
