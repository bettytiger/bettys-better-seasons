package net.bettytiger.bbetter_seasons.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Fired before an out-of-season crop withers. Cancel to keep the crop in place. */
@Cancelable
public class CropWitherEvent extends Event {

    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState state;

    public CropWitherEvent(ServerLevel level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }
}
