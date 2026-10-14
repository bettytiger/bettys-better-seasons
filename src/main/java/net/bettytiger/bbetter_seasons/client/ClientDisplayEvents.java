package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.bettytiger.bbetter_seasons.seasonal.OutOfSeasonRule;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.StringJoiner;

/** Client Forge-bus display handlers: F3 debug lines and seasonal item tooltips. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientDisplayEvents {

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!BetterSeasonsClientConfig.CLIENT.f3Lines.get()) {
            return;
        }
        CalendarDate date = ClientCalendar.getDate();
        List<Season> seasons = ClientCalendar.getSeasons();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (date == null || seasons.isEmpty() || level == null) {
            return;
        }

        String seasonName = date.seasonIndex() >= 0 && date.seasonIndex() < seasons.size()
                ? seasons.get(date.seasonIndex()).displayName().getString()
                : "?";
        String subSeason = Component.translatable(date.subSeason().translationKey()).getString();

        List<String> left = event.getLeft();
        left.add("");
        left.add("[BBS] " + date.weekdayName().getString() + " Day " + date.day()
                + " (" + subSeason + " " + seasonName + ") Y" + date.year());

        int ticksPerDay = Math.max(1, ClientCalendar.getTicksPerDay());
        long intoDay = Math.floorMod(level.getDayTime(), (long) ticksPerDay);
        left.add("[BBS] Day progress: " + (intoDay * 100L / ticksPerDay) + "%");

        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit) {
            var state = level.getBlockState(blockHit.getBlockPos());
            SeasonalCropInfo info = ClientSeasonal.getForBlock(state.getBlock());
            if (info != null) {
                boolean inSeason = BetterSeasonsAPI.isInSeason(level, blockHit.getBlockPos(), state);
                left.add("[BBS] Crop under crosshair: " + (inSeason ? "in season" : "out of season"));
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!BetterSeasonsClientConfig.CLIENT.seasonalTooltips.get()) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        SeasonalCropInfo info = ClientSeasonal.getForItem(stack.getItem());
        if (info == null) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        List<Season> seasons = ClientCalendar.getSeasons();

        if (!info.seasons().isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (Season season : seasons) {
                if (info.seasons().contains(season.id())) {
                    joiner.add(season.displayName().getString());
                }
            }
            boolean inSeason = BetterSeasonsAPI.isInSeason(level, stack.getItem());
            tooltip.add(Component.translatable("tooltip." + BettysBetterSeasonsMod.MODID + ".grows_in", joiner.toString())
                    .withStyle(inSeason ? ChatFormatting.GREEN : ChatFormatting.GRAY));

            if (!inSeason && (info.rule() == OutOfSeasonRule.STOP || info.rule() == OutOfSeasonRule.WITHER)) {
                tooltip.add(Component.translatable("tooltip." + BettysBetterSeasonsMod.MODID + ".out_of_season")
                        .withStyle(ChatFormatting.RED));
            }
        }

        if (info.peak() != null) {
            String peakSeasonName = seasonName(seasons, info.peak().season());
            String peakSub = Component.translatable(info.peak().subSeason().translationKey()).getString();
            tooltip.add(Component.translatable("tooltip." + BettysBetterSeasonsMod.MODID + ".peak", peakSub, peakSeasonName)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static String seasonName(List<Season> seasons, ResourceLocation id) {
        for (Season season : seasons) {
            if (season.id().equals(id)) {
                return season.displayName().getString();
            }
        }
        return id.getPath();
    }
}
