package net.bettytiger.bbetter_seasons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Purely decorative remnant left when a crop withers out of season. Thin, no collision, instantly
 * broken, and survives on the same soils a crop would. Default {@code wither} replacement for crops
 * that do not implement {@code ISeasonalCrop}.
 */
@SuppressWarnings("ALL")
public class WitheredCropBlock extends BushBlock {

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

    public WitheredCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
