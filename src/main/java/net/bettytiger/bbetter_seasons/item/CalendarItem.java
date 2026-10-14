package net.bettytiger.bbetter_seasons.item;

import net.bettytiger.bbetter_seasons.client.BetterSeasonsClientAPI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Handheld calendar, and the item form of the wall calendar: right-click opens the calendar screen;
 * shift + right-click places it as a wall calendar (consumed in survival, given back when broken).
 */
public class CalendarItem extends BlockItem {

    public CalendarItem(Block wallCalendar, Properties properties) {
        super(wallCalendar, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            // Not sneaking: pass so use() opens the calendar screen instead of placing.
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BetterSeasonsClientAPI::openCalendar);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /** Keep the item's own name ("Calendar") rather than the placed block's ("Wall Calendar"). */
    @Override
    public @NotNull String getDescriptionId() {
        return getOrCreateDescriptionId();
    }
}
