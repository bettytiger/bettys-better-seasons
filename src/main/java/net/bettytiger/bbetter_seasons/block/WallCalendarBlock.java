package net.bettytiger.bbetter_seasons.block;

import net.bettytiger.bbetter_seasons.client.BetterSeasonsClientAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Placeable wall calendar. Faces the player when placed; right-click opens the calendar screen.
 * Its collision/outline shape matches the model and the block does not occlude (so neighbor faces
 * aren't culled).
 */
public class WallCalendarBlock extends HorizontalDirectionalBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Model footprint for the default (north-facing) orientation; rotated for the other facings.
    private static final VoxelShape SHAPE_NORTH = Block.box(3.0, 0.0, 14.0, 13.0, 11.5, 16.0);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.NORTH, SHAPE_NORTH);
        SHAPES.put(Direction.EAST, rotateY(SHAPE_NORTH));
        SHAPES.put(Direction.SOUTH, rotateY(rotateY(SHAPE_NORTH)));
        SHAPES.put(Direction.WEST, rotateY(rotateY(rotateY(SHAPE_NORTH))));
    }

    public WallCalendarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the player: the readable side points opposite to where the player is looking.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_NORTH);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BetterSeasonsClientAPI::openCalendar);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Rotates a shape 90° clockwise around Y, matching the blockstate {@code y} rotation. */
    private static VoxelShape rotateY(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.or(result[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return result[0];
    }
}
