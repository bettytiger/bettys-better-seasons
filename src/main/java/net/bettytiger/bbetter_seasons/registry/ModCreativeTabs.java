package net.bettytiger.bbetter_seasons.registry;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the library's items to vanilla creative tabs. */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModRegistry.CALENDAR.get());
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModRegistry.WITHERED_CROP_ITEM.get());
        }
    }
}
