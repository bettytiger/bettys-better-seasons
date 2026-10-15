package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Client mod-bus wiring: the date HUD overlay, seasonal leaf colours, and client config reloads. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("date_hud", new DateHudOverlay());
        event.registerAboveAll("season_transition", SeasonTransition.INSTANCE);
    }

    /**
     * Birch and spruce leaves use fixed colours instead of the biome's foliage colour, so the biome mixin
     * doesn't reach them; tint them here so all leaves follow the season. Inventory items keep vanilla colours.
     */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? SeasonalTint.apply(FoliageColor.getBirchColor()) : FoliageColor.getBirchColor(), Blocks.BIRCH_LEAVES);
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? SeasonalTint.apply(FoliageColor.getEvergreenColor()) : FoliageColor.getEvergreenColor(), Blocks.SPRUCE_LEAVES);
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        // Toggling seasonalColors in the config file takes effect without rejoining.
        if (event.getConfig().getSpec() == BetterSeasonsClientConfig.CLIENT_SPEC) {
            Minecraft.getInstance().execute(SeasonalTint::refresh);
        }
    }
}
