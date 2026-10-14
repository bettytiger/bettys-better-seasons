package net.bettytiger.bbetter_seasons.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired before the growth gate decides whether a crop is in season. Set the result to override:
 * {@link Result#ALLOW} forces in-season, {@link Result#DENY} forces out-of-season,
 * {@link Result#DEFAULT} lets the normal data-driven check run.
 */
@Event.HasResult
public class CropSeasonCheckEvent extends Event {

    private final Level level;
    private final BlockPos pos;
    private final BlockState state;
    private final ResourceLocation currentSeason;

    public CropSeasonCheckEvent(Level level, BlockPos pos, BlockState state, ResourceLocation currentSeason) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.currentSeason = currentSeason;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    public ResourceLocation getCurrentSeason() {
        return currentSeason;
    }
}
