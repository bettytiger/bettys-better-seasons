package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.StringJoiner;

/** Client Forge-bus display handlers: hiding chat during the transition, seasonal tooltips, and seasonal colours. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientDisplayEvents {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        // Hide the chat panel while the season transition fade is playing.
        if (SeasonTransition.isActive() && event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SeasonalTint.reset();
        ClientWeather.reset();
    }

    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        // Fires on respawn and dimension change; a non-seasonal dimension (e.g. the Nether) drops the tint and snow.
        SeasonalTint.refresh();
        ClientWeather.refresh();
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
        } else {
            // An entry with no season limit grows year-round (it may still vary its speed by season).
            tooltip.add(Component.translatable("tooltip." + BettysBetterSeasonsMod.MODID + ".grows_all")
                    .withStyle(ChatFormatting.GREEN));
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
