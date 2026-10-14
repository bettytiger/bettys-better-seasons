package net.bettytiger.bbetter_seasons.seasonal;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.bettytiger.bbetter_seasons.event.CropWitherEvent;
import net.bettytiger.bbetter_seasons.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The growth gate. Intercepts {@link BlockEvent.CropGrowEvent.Pre} and {@link BonemealEvent} to apply
 * each crop's out-of-season rule and growth multipliers, and withers crops lazily on their next tick.
 */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SeasonalGate {

    @SubscribeEvent
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!BetterSeasonsConfig.SERVER.seasonalCropsEnabled.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        SeasonalCropInfo info = infoFor(level, state.getBlock());
        if (info == null) {
            return;
        }

        // Resolve the season state once so CropSeasonCheckEvent fires at most once per grow attempt.
        if (!BetterSeasonsAPI.isInSeason(level, pos, state)) {
            switch (info.rule()) {
                case WITHER -> {
                    wither(level, pos, state);
                    event.setResult(Event.Result.DENY);
                }
                case STOP -> event.setResult(Event.Result.DENY);
                case SLOW -> {
                    if (level.random.nextFloat() > BetterSeasonsConfig.SERVER.slowFactor.get().floatValue()) {
                        event.setResult(Event.Result.DENY);
                    }
                }
                case NONE -> { /* grows normally; season is tooltip-only */ }
            }
            return;
        }

        // In season: apply the per-season / peak growth multiplier. Only slowdowns gate here;
        // speed-ups (> 1) are opt-in by crops reading BetterSeasonsAPI.getGrowthModifier themselves.
        ResourceLocation current = BetterSeasonsAPI.getCurrentSeasonId(level);
        CalendarDate date = BetterSeasonsAPI.getDate(level);
        if (current != null && date != null) {
            float modifier = info.growthMultiplier(current, date.subSeason());
            if (modifier < 1.0f && level.random.nextFloat() > modifier) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!BetterSeasonsConfig.SERVER.seasonalCropsEnabled.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getBlock();
        SeasonalCropInfo info = infoFor(level, state.getBlock());
        if (info == null) {
            return;
        }
        // Bonemeal is blocked for stop/wither crops that are out of season.
        if (!info.rule().allowsGrowthOutOfSeason() && !BetterSeasonsAPI.isInSeason(level, event.getPos(), state)) {
            event.setCanceled(true);
        }
    }

    /** Replaces an out-of-season crop with the withered remnant, or delegates to {@link ISeasonalCrop}. */
    public static void wither(ServerLevel level, BlockPos pos, BlockState state) {
        if (MinecraftForge.EVENT_BUS.post(new CropWitherEvent(level, pos, state))) {
            return;
        }
        Block block = state.getBlock();
        if (block instanceof ISeasonalCrop crop) {
            crop.onOutOfSeason(level, pos, state);
            return;
        }
        if (BetterSeasonsConfig.SERVER.witherDropsSeed.get()) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(block.asItem()));
        }
        level.setBlock(pos, ModRegistry.WITHERED_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static SeasonalCropInfo infoFor(Level level, Block block) {
        SeasonalCropInfo info = SeasonalCropRegistry.getForBlock(block);
        if (info == null && block instanceof ISeasonalCrop crop) {
            return new SeasonalCropInfo(java.util.Set.copyOf(crop.getDefaultSeasons()), crop.getDefaultRule(), java.util.Map.of(), null);
        }
        return info;
    }
}
