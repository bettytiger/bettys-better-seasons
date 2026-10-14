package net.bettytiger.bbetter_seasons.registry;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Converts items that no longer exist in worlds saved by older versions. The separate
 * {@code wall_calendar} item was removed (the calendar is now the wall calendar's item), so any left in
 * inventories or chests become calendars instead of vanishing behind a missing-entries warning.
 */
@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyItemRemaps {

    private LegacyItemRemaps() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, BettysBetterSeasonsMod.MODID)) {
            if (mapping.getKey().getPath().equals("wall_calendar")) {
                mapping.remap(ModRegistry.CALENDAR.get());
            }
        }
    }
}
