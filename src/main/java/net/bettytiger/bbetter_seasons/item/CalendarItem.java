package net.bettytiger.bbetter_seasons.item;

import net.bettytiger.bbetter_seasons.client.BetterSeasonsClientAPI;
import net.bettytiger.bbetter_seasons.registry.ModRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;

/** Handheld calendar: right-click opens the calendar screen; shift + right-click places a wall calendar. */
public class CalendarItem extends Item {

    public CalendarItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            // Not sneaking: fall through so use() can open the calendar screen.
            return InteractionResult.PASS;
        }
        return placeWallCalendar(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BetterSeasonsClientAPI::openCalendar);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /** Places the wall calendar at the clicked face without consuming the calendar item. */
    private static InteractionResult placeWallCalendar(UseOnContext context) {
        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!placeContext.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockState state = ModRegistry.WALL_CALENDAR.get().getStateForPlacement(placeContext);
        if (state == null) {
            return InteractionResult.FAIL;
        }
        if (!level.setBlock(placeContext.getClickedPos(), state, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        SoundType sound = state.getSoundType();
        level.playSound(context.getPlayer(), placeContext.getClickedPos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
